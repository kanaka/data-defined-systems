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
            #_["pg$default" :as pg]))

;; TODO: use :require syntax when shadow-cljs works with "*$default"
(def pg (js/require "pg"))

(defn get-table [client table]
  (P/let [result (.query client (str "SELECT * FROM " table ";"))
          rows (js->clj (.-rows result) :keywordize-keys true)]
    rows))

(defn insert-row [client table row]
  (P/let [ks (S/join ", " (map name (keys row)))
          vs (vals row)
          vnums (S/join ", " (map #(str "$" %1)
                                  (range 1 (inc (count row)))))
          sql (str "INSERT INTO " table " (" ks ")"
                   " VALUES (" vnums ")")
          _ (prn :sql sql)
          _ (prn :vs vs)
          result (.query client sql (clj->js vs))]
    result))

(defn write-haproxy-config
  [{:keys [log-msg haproxy-opts]} ip]
  (when haproxy-opts
    (let [{:keys [output-dir server-port]} haproxy-opts
          svr (str "svr_" (S/replace ip #"[.]" "_"))
          file (str output-dir "/" ip ".cfg")
          line (str "server " svr " " ip ":" server-port " check")]
      (P/do
        (fs/mkdir output-dir #js {:recursive true})
        (log-msg :info "Writing to " file ": " line)
        (fs/writeFile file (str "  " line "\n"))))))

(defn query-or-assign-ip
  [{:keys [pg-opts pg-table dhcp-cfg]} mac]
  (P/let
    [pg-client (doto (pg.Client. (clj->js pg-opts))
                 .connect)
     rows (get-table pg-client pg-table)
     reassigned-ip (:ip (first (filter #(= (:mac %) mac) rows)))
     ip (or reassigned-ip
            (P/let [used-set (set (map :ip rows))
                    all-ips (addrs/ip-seq (:start dhcp-cfg)
                                          (:end dhcp-cfg))
                    assign-ip (some #(if (contains? used-set %) nil %)
                                    all-ips)
                    res (insert-row pg-client pg-table {:mac mac
                                                        :ip assign-ip})]
              assign-ip))]
    (.end pg-client)
    (when ip
      (merge dhcp-cfg {:ip ip
                       :action (if reassigned-ip
                                 "Reassigning"
                                 "Assigning")}))))

(defn pool-handler
  "Takes a parsed DHCP client message `msg-map`, queries the DB
  for assigned IPs or assigns one, and responds to the client with the
  assigned address."
  [{:keys [log-msg server-info] :as cfg} msg-map]
  (P/let [field-overrides (:fields cfg) ;; config file field/option overrides
          mac (:chaddr msg-map)
          dhcp-cfg (query-or-assign-ip cfg mac)]
    (if (not dhcp-cfg)
      (log-msg :error (str "MAC " mac " could not be queried"))
      (P/let [{:keys [action ip gateway netmask]} dhcp-cfg]
        (write-haproxy-config cfg ip)
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

  (let [file-cfg (util/load-config config-file)
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
        cfg (merge
              user-cfg
              {:message-handler pool-handler
               :log-msg logging/log-message})]

    (logging/start-logging cfg)
    (log-msg :info (str "User config: " user-cfg))

    (log-msg :info "Starting DHCP Server...")
    (server/create-server cfg)
    nil))
