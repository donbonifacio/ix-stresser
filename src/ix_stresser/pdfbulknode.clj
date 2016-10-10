(ns ix-stresser.pdfbulknode
  (:require [environ.core :refer [env]]
            [clj-http.client :as client]
            [clojure.core.async :as async :refer [<! <!! go go-loop timeout]]
            [clojure.data.json :as json]))

(def base-files-path "./doc/")
(def dev-url "http://localhost:3000")
(def stag-url (env :pdfnode-stag-url))
(def prod-url (env :pdfnode-prod-url))
(def existing-files ["invoice_small.json", "invoice_medium.json", "invoice_big.json"])

(def document-multiplier 1) ;; 1 will mean a single bulk having 3 invoices, small, medium and big
;; 2 will mean 6 invoices, 2 small, 2 medium , 2 big... and so on...

(def stressing-times 1)
(def requesting-times 1)

(defn url-for [url, endpoint]
  (str url endpoint))

(defn read-file [filename]
  (json/read-str (slurp (str base-files-path filename)) :key-fn keyword))

(defn read-all-files []
  (reduce (fn [hash filename]
            (assoc hash (keyword filename) (read-file filename)))
          {}
          existing-files))

(defn bulk-contents-data []
  (reduce (fn [array filename]
            (->> (read-all-files)
                 ((keyword filename))
                 (conj array)
             ))
          []
          existing-files))

(defn setup-bulk-contents []
  (->> (bulk-contents-data)
       (repeat)
       (take document-multiplier)
       (flatten)
       (map-indexed (fn [file-counter file-data]
                      (assoc file-data :filename (str file-counter ".pdf"))))))

(defn bulk-data []
  { :token (env :pdfnode-token),
    :replyto "http://ricardofiel.com",
    :filename "my_bulk_test",
    :contents (setup-bulk-contents)})

(defn change-file-name [data stress-count request-count]
  (println (data :filename))
  (assoc data :filename (str "bulk_test_" (str stress-count request-count))))

(defn do-request [endpoint data]
  (println ".Calling" endpoint "for the file" (data :filename))
  (client/post endpoint { :content-type :json
                          :body (json/write-str data)
                          :socket-timeout 1000
                          :conn-timeout 1000
                          :accept :json }))

(defn print-response [request-ch]
  (go
    (let [response request-ch]
      (println ".Response" response))))

(defn stress-out [url endpoint bulk-data stress-count]
  (dotimes [n requesting-times]
    (future 
      ;;(try
        (do-request (url-for url endpoint) (change-file-name bulk-data stress-count  n))
        ;;(catch Exception e
          ;;(prn e))))))
      )))

(defn start-stressing [urls endpoint bulk-data]
  (dotimes [n stressing-times]
    (doseq [url urls]
      (stress-out url endpoint bulk-data n)
      (prn "--- sleep 1s" )
      (<!! (timeout 1000)))))

(defn runner [args]
  (let [endpoint (or (first args) "/pdf/applyTemplateBulk")]
    ;; (start-stressing [dev-url] endpoint (bulk-data)) ;; stressing localy
    ;; (start-stressing [stag-url] endpoint (bulk-data)) ;; stressing staging
    ;; (start-stressing [prod-url] endpoint (bulk-data)) ;; stressing production
    (start-stressing [dev-url, stag-url, prod-url] endpoint (bulk-data)) ;; stressing multiple envs
    (prn "OK!")))

(defn -main [& args]
  (runner args))
