(ns ix-stresser.core
  (:require [clj-http.client :as client]
            [invoice-spec.api.documents :as api]
            [invoice-spec.models.document :as document]
            [environ.core :refer [env]]
            [result.core :as result]
            [clojure.core.async :as async :refer [<! <!! go go-loop]]))

(defn create-bulk-documents
  [{:keys [docs-per-thread template] :as options}]
  (println)
  (println "-- Creating documents")
  (take docs-per-thread
    (repeatedly
      (fn []
        (let [document (<!! (api/create options template))]
          (if (result/succeeded? document)
            (do (print ".") (flush))
            (prn (str "Error " document)))
          document)))))

(defn success-result [results]
  (first (filter result/succeeded? results)))

(defn finalize-documents
  [options documents]
  (println)
  (println "-- Finalizing documents")
  (go-loop [documents documents
            final-documents []]
    (let [document (first documents)
          try1 (api/finalize options document)
          try2 (api/finalize options document)
          try3 (api/finalize options document)

          result1 (<! try1)
          result2 (<! try2)
          result3 (<! try3)

          result (success-result [result1 result2 result3])
          sequence-number (:sequence_number result)]

      (if (nil? sequence-number)
        (prn result1 result2 result3)
        (do (print "F") (flush)))
      (if-let [documents (seq (rest documents))]
        (recur documents (conj final-documents result))
        (conj final-documents result)))))

(defn defaults [args]
  (merge {:docs-per-thread 3
          :template (-> (document/new-invoice)
                        (assoc :date "21/07/2016")
                        (assoc :due_date "21/07/2016"))
          :host (or (:host args) (env :ix-api-host))
          :port (or (:port args) (env :ix-api-port) "3001")
          :api-key (or (:api-key args) (env :ix-api-key))}
         args))

(defn extract-document-number [document]
  (let [sequence-number (:sequence_number document)
        parts (clojure.string/split sequence-number #"/")]
    (read-string (first parts))))

(defn verify-finalized [documents]
  (let [numbers (->> documents
                     (map extract-document-number)
                     (apply sorted-set))
        start (first numbers)
        end (last numbers)]
    (println)
    (println "-- Finalize resul")
    (println "start:" start)
    (println "end:" end)
    (println "count:" (count numbers))
    (assert (= (count numbers) (count documents)))
    (assert (= (+ start (count documents) -1) end))
    (result/success)))

(defn prn-options [options]
  (prn "-- Options")
  (prn ":api-path" (:host options) ":" (:port options) "/?api_key=" (:api-key options))
  (prn ":threads" 1)
  (prn ":docs-per-thread" (:docs-per-thread options)))

(defn runner [args]
  (<!! (go
    (let [options (defaults args)]
      (prn-options options)
      (let [documents (doall (create-bulk-documents options))
            finalized (<! (finalize-documents options documents))
            finalize-result (verify-finalized finalized)]
        (prn "OK!"))))))
