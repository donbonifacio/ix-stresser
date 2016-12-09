(ns ix-stresser.estimates
  (:require [clj-http.client :as client]
            [invoice-spec.api.documents :as api]
            [invoice-spec.models.document :as document]
            [environ.core :refer [env]]
            [result.core :as result]
            [ix-stresser.core :as core]
            [clojure.core.async :as async :refer [<! <!! go go-loop]]))

(defn new-estimate []
  (-> (document/new-invoice)
      (assoc :type "Quote")
      (assoc :mb_reference "1")
      (assoc :auto_add_related_document "1")
      (assoc :date "21/09/2016")
      (assoc :due_date "21/09/2016")))

(defn runner [args]
  (<!! (go
    (let [options (core/defaults args)
          options (assoc options :template (new-estimate)
                         :docs-per-thread 5)]
      (core/prn-options options)
      (let [documents (doall (core/create-bulk-documents options))
            finalized (<! (core/finalize-documents options documents))
            ]
        (prn "Docs: " (map :sequence_number finalized))
        (prn "OK!"))))))

(defn -main [& args]
  (runner {}))
