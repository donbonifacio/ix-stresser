(ns ix-stresser.core
  (require [clj-http.client :as client]))

(def url "http://boss.bizflow.com")
(def ix-api-key "donbonifacio")
(def ports [3001 3002 3003])

(defn access-url
  [path]
  (str url ":" (rand-nth ports) "/" path "?api_key=" ix-api-key))

(defn invoice-sequence-from-response
  [response]
  (let [body (:body response)
        [match raw-id] (re-matches #"(?sm).*<sequence_number>(.+)</sequence_number>.*" body)]
    raw-id))

(defn invoice-id-from-response
  [response]
  (let [body (:body response)
        clean-body (clojure.string/replace body "\n" "")
        [match raw-id] (re-matches #".*<invoice>  <id>(\d+)</id>.*" clean-body)]
    (read-string raw-id)))

(defn create-invoice
  []
  (try
    (client/post (access-url "invoices.xml")
                 {:content-type :xml
                  :socket-timeout 0
                  :conn-timeout 0
                  :throw-exceptions false
                  :accept :xml
                  :body (str "<?xml version='1.0' encoding='UTF-8'?>
                        <invoice>
                        <date>14/07/2016</date>
                        <due_date>14/07/2016</due_date>
                        <client>
                          <name>Bruce Norris</name>
                        </client>
                        <items type='array'>
                          <item>
                          <name>Product 1</name>
                          <description>Cleaning product</description>
                          <unit_price>10.0</unit_price>
                          <quantity>1.0</quantity>
                          <unit>unit</unit>
                          <discount>10.0</discount>
                          </item>
                        </items>
                        </invoice>")})
    (catch Exception e nil)))

(defn finalize-invoice
  [invoice-id]
  (try
   (client/put  (access-url (str "invoice/" invoice-id "/change-state.xml"))
               {:content-type :xml
                :socket-timeout 0
                :conn-timeout 0
                :accept :xml
                :throw-exceptions false
                :body (str "<invoice>
                            <state>finalized</state>
                            </invoice>")})
    (catch Exception e nil)))
