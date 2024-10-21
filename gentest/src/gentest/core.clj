(ns gentest.core
  (:require [clojure.string :as S]
            [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]

            [docopt.core :as docopt]
            [org.httpkit.client :as hk]
            [instaparse.core :as instaparse]
            [instacheck.grammar :as igrammar]
            [instacheck.cli :as icli]))

(def usage "HTTP Endpoint Generative Tester

Usage:
  gentest check [OPTIONS] <dest1> <dest2>
  gentest run [OPTIONS] <dest1> <dest2> <input-file>
  gentest parse [OPTIONS] <inputs>...
  gentest samples [OPTIONS]

Options:
  -h --help           Show this help
  -v --verbose        Verbose output
  --ebnf-file=FILE    EBNF file that defines HTTP requests
                      [default: ./actions.ebnf]
  --ebnf-output=FILE  EBNF file that defines HTTP requests
                      [default: ./actions.ebnf]
  --runs=NUM          Number of test runs (check or run)
                      [default: 1]
  --iterations=NUM    Number of test iterations per run
                      [default: 10]
  --count=NUM         Number of samples to generate
                      [default: 10]
  --output-dir=DIR    Output samples/inputs to this dir
                      [default: output]
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

(defn run-action [ctx dest1 dest2 action]
  (let [{:keys [method path payload]} action
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
      (prn :run-action :method method :path path
           :statuses (map :status [resp1 resp2]) :-> pass?))
    pass?))

(defn run-actions [ctx dest1 dest2 actions-file]
  (let [actions-str (slurp actions-file)
        actions (json/read-str actions-str :key-fn keyword)
        all-actions (concat [{:method "PUT" :path "/reset"}]
                            actions
                            [{:method "GET" :path "/users"}])]
    (loop [actions all-actions]
      (let [[action & actions] actions
            pass? (run-action ctx dest1 dest2 action)]
        (if (and pass? (seq actions))
          (recur actions)
          pass?)))))

(defn inner-main [opts]
  (let [{:keys [check run parse samples verbose runs iterations count
                weights-in ebnf-file ebnf-output output-dir
                dest1 dest2 inputs input-file]} opts
        [runs iterations count] (map #(Integer. %) [runs iterations count])
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
                     {:log-fn icli/pr-err}))
        check-fn (fn [ctx input-file]
                   (run-actions ctx dest1 dest2 input-file))]
    (cond
      check
      (icli/do-check ctx actions-parser output-dir check-fn
                     (merge opts {:runs       runs
                                  :iterations iterations}))

      run
      (let [fails (atom 0)]
        (dotimes [n runs]
          (print (str "RUN " n " -> ")) (flush)
          (if (run-actions ctx dest1 dest2 input-file)
            (do (println "PASS"))
            (do (println "FAIL"))))
        (System/exit (if (= 0 @fails) 0 1)))

      parse
      (icli/do-parse ctx actions-parser inputs)

      samples
      (icli/do-samples ctx actions-parser output-dir count))))

(defn -main [& args]
  (docopt/docopt usage args #(inner-main (clean-opts %))))
