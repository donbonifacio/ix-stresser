(ns ix-stresser.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [invoice-spec.api.documents :as api]
            [invoice-spec.models.document :as document]
            [invoice-spec.api.common :as common]
            [environ.core :refer [env]]
            [result.core :as result]
            [ix-stresser.server :as server]
            [request-utils.core :as request-utils]
            [clojure.core.async :as async :refer [<! <!! go go-loop]]))

(def current-port-index (atom 0))

(defn new-invoice []
  (-> (document/new-invoice)
      (assoc :date "21/01/2017")
      (assoc :due_date "21/01/2017")))

(defn balancer [options]
  (let [ix-ports (:ix-ports options)
        lucky (swap! current-port-index inc)
        current-index (mod lucky (count ix-ports))]
    #_(prn lucky current-index (nth ix-ports current-index))
    (assoc options :port (nth ix-ports current-index))))

(defn create-bulk-documents
  [{:keys [docs-per-thread template] :as options}]
  (println)
  (println "-- Creating documents")
  (take docs-per-thread
    (repeatedly
      (fn []
        (let [template (or template (new-invoice))
              document (<!! (api/create (balancer options) template))]
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

          try1 (api/finalize (balancer options) document)
          try2 (api/finalize (balancer options) document)
          try3 (api/finalize (balancer options) document)

          result1 (<! try1)
          result2 (<! try2)
          result3 (<! try3)

          result (success-result [result1 result2 result3])
          sequence-number (:sequence_number result)]

      (if (nil? sequence-number)
        (prn result1 result2 result3)
        (do (print ".") (flush)))
      (if-let [documents (seq (rest documents))]
        (recur documents (conj final-documents result))
        (conj final-documents result)))))

(defn finalize-parallel-documents
  [options documents]
  (println)
  (println "-- Finalizing all documents in parallel")
  (go
    (try
      (->> documents
           (pmap #(do (do (print ".") (flush))
                      (api/finalize (balancer options) %)))
           (mapv <!!))
      (catch Exception ex
        (println ex)))))

(defn settle-documents [options documents]
  (println)
  (println "-- Settling documents")
  (go-loop [documents documents
            final-documents []]
    (let [document (first documents)

          try1 (api/settle (balancer options) document)
          try2 (api/settle (balancer options) document)
          try3 (api/settle (balancer options) document)

          result1 (<! try1)
          result2 (<! try2)
          result3 (<! try3)

          result (success-result [result1 result2 result3])
          sequence-number (:sequence_number result)]

      (if (nil? sequence-number)
        (prn result1 result2 result3)
        (do (print ".") (flush)))
      (if-let [documents (seq (rest documents))]
        (recur documents (conj final-documents result))
        (conj final-documents result)))))

(defn ix-ports []
  (if-let [raw (env :ix-ports)]
    (clojure.string/split raw #",")
    ["3001"]))

(defn- ix-auto-create-account? []
  (= "true" (env :ix-auto-create-account)))

(defn- ix-start-instances? []
  (= "true" (env :ix-start-instances)))

(defn defaults [args]
  (merge {:docs-per-thread 30
          :threads 1
          :simul-state-changes 3
          :ix-ports (ix-ports)
          :ix-server-dir (or (:ix-server-dir args) (env :ix-server-dir) ".")
          :template (new-invoice)
          :host (or (:host args) (env :ix-api-host))
          :port (or (:port args) (env :ix-api-port) "3001")
          :api-key (or (:api-key args) (env :ix-api-key))}
         args))

(defn extract-document-number [document]
  (try
    (let [sequence-number (:sequence_number document)
          parts (clojure.string/split sequence-number #"/")]
      (read-string (first parts)))
    (catch Exception ex
      (println "Error getting doc number" document ex))))

(defn verify-finalized [documents]
  (let [numbers (->> documents
                     (map extract-document-number)
                     (apply sorted-set))
        start (first numbers)
        end (last numbers)]
    (println)
    (println "-- Finalize" (-> documents first :type) "result")
    (println "start:" start)
    (println "end:" end)
    (println "count:" (count numbers))
    (assert (= (count numbers) (count documents)))
    (assert (= (+ start (count documents) -1) end) numbers)
    (result/success)))

(defn verify-settled [options documents]
    (println)
    (println "-- Settled result")
    (loop [documents documents
           receipts []]
      (when-let [document (first documents)]
        (assert (= "settled" (:status document)))
        (let [related (<!! (api/related-documents (balancer options) document))
              receipts (conj receipts (-> related :documents first))]
          (if (seq (rest documents))
            (recur (rest documents) receipts)
            (verify-finalized receipts)
            )
          ))))

(defn prn-options [options]
  (prn "-- Options")
  (prn ":ix-ports" (:ix-ports options))
  (prn ":ix-server-dir" (:ix-server-dir options))
  (prn ":api-path" (:host options) ":" (:port options) "/?api_key=" (:api-key options))
  (prn ":threads" (:threads options))
  (prn ":docs-per-thread" (:docs-per-thread options))
  (prn ":simul-state-changes" (:simul-state-changes options)))

(defn- setup [options]
  (when (ix-start-instances?)
    (server/start-instances options)
    (server/wait-instances options))

  (if (ix-auto-create-account?)
    (let [port (:port (balancer options))
          request-xml (str"<account>
                            <organization_name>ix-stresser</organization_name>
                            <email>" (System/currentTimeMillis) (str (java.util.UUID/randomUUID))  "@example.com</email>
                            <password>123456</password>
                            <terms>1</terms>
                          </account>")
          result (<!! (request-utils/http-post
                         {:host (str "http://localhost:"port"/api/accounts/create.xml")
                          :plain-body? true
                          :headers {"Content-type" "application/xml; charset=utf-8"}
                          :body request-xml}))]
      (println "Creating account...")
      (when (result/failed? result)
        (println request-xml)
        (println result)
        (System/exit 1))
      (let [data (common/load-from-xml (:body result))]
        (println data)
        (assoc options :api-key (:api_key data))))
    options))

(defn runner [args]
  (<!!
    (go
      (try
        (let [options (-> args (defaults) (setup))]
          (prn-options options)
          (let [documents (doall (create-bulk-documents options))
                finalized (<! (finalize-documents options documents))
                finalize-result (verify-finalized finalized)
                settled (<! (settle-documents options finalized))
                settle-result (verify-settled options settled)
                ]
            (shutdown-agents)
            (prn "OK!")))
        (catch Exception ex
          (println ex))))))

(defn runner-parallel-distinct-finalize [& args]
  (<!! (go
    (let [options (-> args (defaults) (setup))]
      (prn-options options)
      (let [documents (doall (create-bulk-documents options))
            finalized (<! (finalize-parallel-documents options documents))
            finalize-result (verify-finalized finalized)
            ]
        (prn "OK!"))))))

(defn -main [& args]
  (runner {}))
