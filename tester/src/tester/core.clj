(ns tester.core
  (:require [clojure.string :as S]
            [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]

            [docopt.core :as docopt]
            [org.httpkit.client :as hk]
            [instaparse.core :as instaparse]
            [instacheck.grammar :as igrammar]
            [instacheck.cli :as instacheck]))

(def usage "HTTP Endpoint Tester

Usage:
  tester check [OPTIONS] <dest1> <dest2>
  tester parse [OPTIONS] <inputs>...

Options:
  -h --help           Show this help
  -v --verbose        Verbose output
  --ebnf-file=FILE    EBNF file that defines HTTP requests
                      [default: ./actions.ebnf]
  --ebnf-output=FILE  EBNF file that defines HTTP requests
                      [default: ./actions.ebnf]
  --runs=NUM          Number of test runs
                      [default: 1]
  --iterations=NUM    Number of test iterations per run
                      [default: 10]
  --sample-dir=DIR    Output samples/inputs to this dir
                      [default: samples]
  --weights-in=FILE   Weights file (EDN) to override EBNF weights
  --weights-out=FILE  Output weights file (EDN) with fail case weights.
")

(defn clean-opts [arg-map]
  (reduce (fn [o [a v]]
            (let [k (keyword (S/replace a #"^[-<]*([^>]*)[>]*$" "$1"))]
              (assoc o k (or (get o k) v))))
          {} arg-map))

(defn body->json
  [body]
  (when (and body (not= "" body)) (json/read-str body)))

(defn url [base path]
  (.toString (.resolve (java.net.URI. base) path)))

(defn run-actions [ctx dest1 dest2 sample-path]
  (let [actions-str (slurp sample-path)
        actions (json/read-str actions-str :key-fn keyword)
        all-actions (concat [{:method "PUT" :path "/reset"}]
                            actions
                            [{:method "GET" :path "/users"}])]
    (loop [actions all-actions]
      (let [[action & actions] actions
            {:keys [method path payload]} action
            base {:method (keyword (S/lower-case method))
                  :headers {"Content-Type" "application/json"}
                  :body (when payload (json/write-str payload))}

            req1 (hk/request (assoc base :url (url dest1 path)))
            req2 (hk/request (assoc base :url (url dest2 path)))
            [resp1 resp2] [@req1 @req2]
            [body1 body2] (map (comp body->json :body) [resp1 resp2])
            pass? (and (= (:status resp1) (:status resp2))
                        (= body1 body2))]
        (when (:verbose ctx)
          (prn :run-actions :method method :path path
               :statuses (map :status [resp1 resp2]) :-> pass?))
        (if (and pass? (seq actions))
          (recur actions)
          pass?)))))

(defn run [opts]
  (let [{:keys [check parse verbose runs iterations
                weights-in ebnf-file sample-dir
                dest1 dest2
                ebnf-output inputs]} opts
        _ (when verbose (println "Opts:" (pprint opts)))
        file-weights (when weights-in (edn/read-string (slurp weights-in)))
        actions-parser (instaparse/parser (slurp ebnf-file))
        comment-weights (igrammar/comment-trek
                          (igrammar/parser->grammar actions-parser)
                          :weight)
        ctx (merge {:verbose verbose
                    :weights (merge comment-weights file-weights)
                    :weights-res (atom {})
                    :ebnf-output ebnf-output}
                   (when (:verbose opts)
                     {:log-fn instacheck/pr-err}))
        check-fn (fn [ctx sample-path]
                     (run-actions ctx dest1 dest2 sample-path))]
    (cond
      check
      (instacheck/do-check ctx actions-parser sample-dir check-fn
                           (merge opts {:runs (Integer. runs)
                                        :iterations (Integer. iterations)}))

      parse
      (instacheck/do-parse ctx actions-parser inputs))))

(defn -main [& args]
  (docopt/docopt usage args #(run (clean-opts %))))
