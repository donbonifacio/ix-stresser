(ns ix-stresser.pdfnode
  (:require [clj-http.client :as client]
            [clojure.core.async :as async :refer [<! <!! go go-loop timeout]]
            [clojure.data.json :as json]))

(def base-files-path "./doc/")
(def dev-url "http://localhost:3000")
(def stag-url "http://pdfnode-staging.cloudapp.net")
(def prod-url "http://pdfnode.cloudapp.net")
(def existing-files ["invoice_small.json", "invoice_medium.json", "invoice_big.json"])
(def stressing-times 1)
(def requesting-times 1)

(defn url-for [url, endpoint]
  (str url endpoint))

(defn read-file [filename]
  (slurp (str base-files-path filename)))

(defn read-all-files []
  (reduce (fn [hash filename]
            (assoc hash (keyword filename) (read-file filename)))
          {}
          existing-files))

(defn do-request [endpoint data]
  (println ".Calling" endpoint)
  (client/post endpoint { :content-type :json
                          :body data
                          :socket-timeout 1000
                          :conn-timeout 1000
                          :accept :json }))

(defn print-response [request-ch]
  (go
    (let [response request-ch]
      (println ".Response" response))))

(defn stress-out [url endpoint files-data]
  (doseq [filename existing-files]
    (dotimes [n requesting-times]
      (future (do-request (url-for url endpoint) (files-data (keyword filename)) )))))

(defn start-stressing [urls endpoint files-data]
  (dotimes [n stressing-times]
    (doseq [url urls]
      (stress-out url endpoint files-data)
      (prn "--- sleep 1s" )
      (<!! (timeout 1000)))))

(defn runner [args]
  (let [endpoint (or (first args) "/pdf/applyTemplate")]
    (start-stressing [dev-url] endpoint (read-all-files)) ;; stressing localy
    ;; (start-stressing [stag-url] endpoint (read-all-files)) ;; stressing staging
    ;; (start-stressing [prod-url] endpoint (read-all-files)) ;; stressing production
    ;; (start-stressing [stag-url, prod-url] endpoint (read-all-files)) ;; stressing multiple envs
    (prn "OK!")))

(defn -main [& args]
  (runner args))
