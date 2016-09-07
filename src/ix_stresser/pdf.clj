(ns ix-stresser.pdf
  (:require [clj-http.client :as client]
            [invoice-spec.api.documents :as api]
            [invoice-spec.models.document :as document]
            [environ.core :refer [env]]
            [result.core :as result]
            [ix-stresser.core :as core]
            [clojure.core.async :as async :refer [<! <!! go go-loop timeout]]))

(defn download-pdfs [options documents]
  (println)
  (println "-- Downloading pdfs")
  (go-loop [documents documents]
    (let [document (first documents)
          remaining (seq (rest documents))
          result (<!! (api/download-pdf options document))]
      (if (result/succeeded? result)
        (do
          (println (:pdfUrl result) " => " (str (:id document) ".pdf"))
          (spit (str (:id document) ".pdf") (slurp (:pdfUrl result))))
        (println "Timed out... " (:id document)))
      (if remaining
        (recur remaining)))))

(defn runner [args]
  (<!! (go
    (let [options (-> (core/defaults args) (dissoc :template))]
      (core/prn-options options)
      (let [documents (doall (core/create-bulk-documents options))
            finalized (<! (core/finalize-documents options documents))
            wait-a-bit (<! (timeout 10000))
            downloads (<! (download-pdfs options finalized))]
        )
      )))
  (shutdown-agents))

(defn -main [& args]
  (runner {}))
