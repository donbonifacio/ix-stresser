(ns ix-stresser.core-test
  (:require [clojure.test :refer :all]
            [ix-stresser.server :as server]
            [ix-stresser.core :refer :all]))

(deftest runner-test
  (let [options (defaults {:ix-ports ["3001" "3002" "3003"]})]
    (server/start-instances options)
    (server/wait-instances options)
    #_(runner options)))


#_(defn create-and-finalize
  []
  (-> (create-many-documents)
      (finalize-documents)))

#_(deftest stress-invoices
  (println 
    (map #(deref %)
      (doall (repeatedly threads (fn [] (future (create-and-finalize))))))))
