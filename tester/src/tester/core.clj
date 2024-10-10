(ns tester.core
  (:require [clojure.string :as S]
            [clojure.data.json :as json]
            [clojure.pprint :refer [pprint]]
            [org.httpkit.client :as hk]
            [instacheck.core :as instacheck]))

(defn body->json [body] (if (= "" body) nil (json/read-str body)))

(defn run-actions [actions-str dest1 dest2]
  (let [actions (json/read-str actions-str :key-fn keyword)]
    (loop [actions actions]
      (let [[action & actions] actions
            {:keys [method path payload]} action
            base {:method (keyword (S/lower-case method))
                  :body (json/write-str payload)}

            req1 (hk/request (assoc base :url (str dest1 path)))
            req2 (hk/request (assoc base :url (str dest2 path)))
            [resp1 resp2] [@req1 @req2]
            [body1 body2] (map (comp body->json :body) [resp1 resp2])
            result (and (= (:status resp1) (:status resp2))
                        (= body1 body2))]
        ;;(prn :RUN :method method :path path :statuses (map :status [resp1 resp2]) :-> result)
        (if (and result (seq actions))
          (recur actions)
          result)))))

(defn report-fn [result]
  #(println :report (select-keys % [:failed-after-ms
                                    :num-tests
                                    :type
                                    :seed
                                    :fail
                                    :failing-size
                                    :pass?
                                    ;;:property
                                    :result
                                    :result-data
                                    :shrinking])))

(defn -main [ebnf-file dest1 dest2 & args]
  (let [ebnf (slurp ebnf-file)
        actions-gen (instacheck/ebnf->gen ebnf)
        result (instacheck/instacheck
                 #(run-actions % dest1 dest2) actions-gen {:iterations 10
                                                           :report-fn report-fn})]
    (println "Result:")
    (pprint result)))
