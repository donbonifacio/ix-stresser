(ns ix-stresser.core-test
  (:require [clojure.test :refer :all]
            [ix-stresser.core :refer :all]))

(def threads 1)
(def docs-per-thread 1)

#_(deftest create-finalinvoice-test
  (let [response (create-invoice)
        invoice-id (invoice-id-from-response response)]
    (is (< 0 invoice-id))
    (let [response (finalize-invoice invoice-id)
          sequence-number (invoice-sequence-from-response response)]
      (is sequence-number))))

(defn create-many-documents
  []
  (println (str "Creating " docs-per-thread " docs..."))
  (take docs-per-thread
    (repeatedly
      (fn []
        (let [resp1 (create-invoice)
            invoice-id (invoice-id-from-response resp1)]
          (prn (str "Created " invoice-id))
          invoice-id)))))

(defn finalize-documents
  [doc-ids]
  (println (str "Finalizing " (count doc-ids) " docs..."))
  (-> (map (fn [invoice-id]
             (let [resp2 (future (finalize-invoice invoice-id))
                   resp3 (future (finalize-invoice invoice-id))
                   resp1 (finalize-invoice invoice-id)
                   seq-number (or (invoice-sequence-from-response @resp2)
                                  (invoice-sequence-from-response @resp3)
                                  (invoice-sequence-from-response resp1))]
               seq-number)) doc-ids)))

(defn create-and-finalize
  []
  (-> (create-many-documents)
      (finalize-documents)))

(deftest stress-invoices
  (println 
    (map #(deref %)
      (doall (repeatedly threads (fn [] (future (create-and-finalize))))))))
