(ns ix-stresser.basic-requests
  (:require [clj-http.client :as client]
            [invoice-spec.api.documents :as api]
            [invoice-spec.models.document :as document]
            [environ.core :refer [env]]
            [result.core :as result]
            [ix-stresser.core :as core]
            [request-utils.core :as request-utils]
            [clojure.core.async :as async :refer [<! <!! go go-loop timeout]]))


(defn runner [args]
  (let [options (core/defaults args)]
    (dotimes [n 150]
      (let [options (core/balancer options)
            url (str (:host options) ":" (:port options) "/")
            response (<!! (request-utils/http-get {:host url :plain-body? true}))]
        (println n ": " url " " (:status response) (:request-time response))))
    (println "End!")
    (shutdown-agents)))

(defn -main [& args]
  (runner {}))
