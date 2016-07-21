(ns ix-stresser.server
  (:require [environ.core :refer [env]]
            [result.core :as result]
            [clojure.java.shell :as shell]
            [request-utils.core :as request-utils]
            [clojure.core.async :as async :refer [<! <!! go go-loop]]))

(defn start-instance [options port]
  (shell/with-sh-dir (:ix-server-dir options)
    (println (str "Starting server on port " port " at dir " (:ix-server-dir options)))
    (prn (shell/sh "script/webserver" "-p" port "-e" "production"))))

(defn start-instances [options]
  (doseq [port (:ix-ports options)]
    (future (start-instance options port))))

(defn wait-instances [options]
  (println "Waiting for servers...")
  (doseq [port (:ix-ports options)]
    (loop []
      (let [result (<!! (request-utils/http-get {:host (str (:host options) ":" port "/")
                                                 :plain-body? true}))]
        (when (result/failed? result)
          (Thread/sleep 1000)
          (print "*")
          (flush)
          (recur)))))
  (println)
  (println "-- Servers ready!"))
