(ns ix-stresser.core-test
  (:require [clojure.test :refer :all]
            [ix-stresser.core :refer :all]))

(deftest runner-test
  (runner {}))


#_(defn create-and-finalize
  []
  (-> (create-many-documents)
      (finalize-documents)))

#_(deftest stress-invoices
  (println 
    (map #(deref %)
      (doall (repeatedly threads (fn [] (future (create-and-finalize))))))))
