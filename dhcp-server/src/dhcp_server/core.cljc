(ns dhcp-server.core
  (:require [promesa.core :as P]
            [clojure.string :as S]
            ;;[clojure.set :refer [difference]]
            [cljs.pprint :refer [pprint]]
            [protocol.addrs :as addrs]
            [dhcp.core :as dhcp]
            [dhcp.util :as util]
            [dhcp.logging :as logging]
            [dhcp.node-server :as server]
            ["fs/promises" :as fs]
            ["nats" :as nats]
            #_["pg$default" :as pg]))

;; TODO: use :require syntax when shadow-cljs works with "*$default"
(def pg (js/require "pg"))

(defn pg-select-all
  [client table]
  (P/let [result (.query client (str "SELECT * FROM " table ";"))]
    (js->clj (.-rows result) :keywordize-keys true)))

(defn pg-insert-row
  [client table row]
  (P/let [ks (S/join ", " (map name (keys row)))
          vnums (S/join ", " (map #(str "$" %1) (range 1 (inc (count row)))))
          sql (str "INSERT INTO " table " (" ks ")" " VALUES (" vnums ")")
          result (.query client sql (clj->js (vals row)))]
    result))

(defn query-or-assign-ip
  [{:keys [pg-opts pg-table dhcp-cfg]} mac]
  (P/let
    [pg-client (doto (pg.Client. (clj->js pg-opts))
                 .connect)
     rows (pg-select-all pg-client pg-table)
     reassign-ip (:ip (first (filter #(= (:mac %) mac) rows)))
     ip (or reassign-ip
            (P/let [used-set (set (map :ip rows))
                    all-ips (addrs/ip-seq (:start dhcp-cfg) (:end dhcp-cfg))
                    assign-ip (some #(if (contains? used-set %) nil %)
                                    all-ips)
                    res (pg-insert-row pg-client pg-table {:mac mac
                                                           :ip assign-ip})]
              assign-ip))]
    (.end pg-client)
    (when ip
      (merge dhcp-cfg {:ip ip
                       :action (if reassign-ip "Reassigning" "Assigning")}))))

(defn nats-publish
  [client subject data]
  (P/let [sc (nats/StringCodec)
          msg (.encode sc (js/JSON.stringify (clj->js data)))]
    (.publish client subject msg)))

(defn pool-handler
  "Takes a parsed DHCP client message `msg-map`, queries the DB
  for assigned IPs or assigns one, sends a NAT event, and then
  responds to the client with the assigned address."
  [{:keys [log-msg server-info nats-cfg nats-client] :as cfg} msg-map]
  (P/let [field-overrides (:fields cfg) ;; config file field/option overrides
          mac (:chaddr msg-map)
          dhcp-cfg (query-or-assign-ip cfg mac)]
    (if (not dhcp-cfg)
      (log-msg :error (str "MAC " mac " could not be queried"))
      (P/let [{:keys [action ip gateway netmask]} dhcp-cfg]
        (when nats-client
          (let [{:keys [server subject target-port]} nats-cfg
                msg {:action "add"
                     :target (str ip ":" target-port)}]
            (log-msg :info (str "Publishing to '" server "': " msg))
            (nats-publish nats-client subject msg)))
        (log-msg :info (str action " " ip "/" netmask " to " mac
                            (when gateway " (gateway " gateway ")")))
        (merge
          (dhcp/default-response msg-map server-info)
          (select-keys msg-map [:giaddr :opt/relay-agent-info])
          {:yiaddr ip
           :opt/netmask netmask}
          (when gateway {:opt/router [gateway]})
          field-overrides)))))

(defn prefix->netmask
  [prefix]
  (let [full-mask 0xFFFFFFFF
        mask (bit-shift-left full-mask (- 32 prefix))
        octets (map #(bit-and 0xFF (bit-shift-right mask %)) [24 16 8 0])]
    (addrs/octet->ip octets)))

(defn main
  "Start a DHCP server listening on `if-name`. Assigning IPs based
  config file settings and tracking assigned IPs via postgres table"
  [config-file & [args]]

  (when-not config-file
    (util/fatal 2 "Must specify a config file"))

  (P/let
    [file-cfg (util/load-config config-file)
     log-msg logging/log-message

     _ (when-not (:if-name file-cfg)
         (util/fatal 2 "config file missing :if-name"))
     _ (when-not (:pg-opts file-cfg)
         (util/fatal 2 (str "config file missing :pg-opts")))
     _ (doseq [opt [:start :end :netmask]]
         (when-not (get-in file-cfg [:dhcp-cfg opt])
           (util/fatal 2 (str "config file missing :dhcp-cfg " opt))))

     ;; precedence: CLI opts, file config, discovered interface info
     if-info (util/get-if-ipv4 (:if-name file-cfg))
     user-cfg (util/deep-merge {:server-info if-info
                                ;;:disable-broadcast true
                                :log-level 2}
                               file-cfg)

     nats-client (when-let [svr (get-in file-cfg [:nats-cfg :server])]
                   (nats/connect #js {:servers svr}))
     cfg (merge
           user-cfg
           {:message-handler pool-handler
            :log-msg logging/log-message
            :nats-client nats-client})]

    (logging/start-logging cfg)
    (log-msg :info (str "User config: " user-cfg))

    (log-msg :info "Starting DHCP Server...")
    (server/create-server cfg)
    nil))
