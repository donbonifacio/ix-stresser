(ns ix-stresser.estimates
  (:require [clj-http.client :as client]
            [clojure.java.jdbc :as j]
            [invoice-spec.api.documents :as api]
            [invoice-spec.models.document :as document]
            [environ.core :refer [env]]
            [result.core :as result]
            [ix-stresser.core :as core]
            [request-utils.core :as request-utils]
            [clojure.core.async :as async :refer [<! <!! go go-loop]]))

(def local-db {:host "localhost"
               :dbname "small-ix"
               :dbtype "mysql"
               :user "root"})

(defn new-estimate []
  (-> (document/new-invoice)
      (assoc :type "Quote")
      (assoc :mb_reference "1")
      (assoc :auto_add_related_document "1")
      (assoc :date "21/09/2016")
      (assoc :due_date "21/09/2016")))

(defn generate-notifications [options]
  (let [references (j/query local-db ["select * from easypay_client_references where payed_at is null order by id desc"])]
    (doseq [reference references]
      (println "Notification for" (:token reference))
      (let [qs (str "ep_doc=" (:id reference) "&ep_value=" (:value reference))
            port (:port (core/balancer options))
            result (<!! (request-utils/http-post
                           {:host (str "http://localhost:"port"/easypay_mb_notifier.xml?" qs)
                            :plain-body? true
                            :body {}}))]
        (when (result/failed? result)
          (prn "---" qs reference)
          (prn result))))))

(defn get-invoices [documents]
  (let [total (count documents)
        invoices (j/query local-db [(str "select * from invoices
                                     where owner_invoice_id in (" (clojure.string/join "," (map :id documents))")
                                     and type is null
                                     order by sequence_number asc")])]
    #_(prn (clojure.string/join "," (map :id documents)))
    #_(prn (clojure.string/join "," (map :id invoices)))
    (if (= total (count invoices))
      invoices
      (do
        (println "Waiting for invoices... got" (count invoices) "/" total "sleeping... ")
        (Thread/sleep 5000)
        (recur documents)))))

(defn runner [args]
  (<!! (go
         (let [options (core/defaults args)
               options (assoc options :template (new-estimate)
                              :docs-per-thread 100)]
           (core/prn-options options)
           (let [documents (doall (core/create-bulk-documents options))
                 finalized (<! (core/finalize-documents options documents))
                 _ (generate-notifications options)
                 invoices (get-invoices documents)
                  _ (prn "Quotes: " (map :sequence_number finalized))
                 _ (core/verify-finalized invoices)
                 ]
             (prn "Invoices " (map :sequence_number invoices))
             (prn "OK!"))))))

(defn -main [& args]
  (runner {}))
