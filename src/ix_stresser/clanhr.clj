(ns ix-stresser.clanhr
  (require [clj-http.client :as client]))

(def base-url "http://directory.api.staging.clanhr.com")

(defn url-for
  [path]
  (str base-url path))

(defn do-login
  []
  (client/post (url-for "/login")
               {:content-type :json
                :socket-timeout 0
                :conn-timeout 0
                :throw-exceptions false
                :accept :json
                :body (str "{\"email\": \"test@test.com\", \"password\": \"test\"}")}))

(defn do-failed-login
  []
  (client/post (url-for "/login")
               {:content-type :json
                :socket-timeout 0
                :conn-timeout 0
                :throw-exceptions false
                :accept :json
                :body (str "{\"email\": \"donbonifacio@gmail.com\", \"password\": \"assobio\"}")}))

(defn do-failed-new-registration
  []
  (client/post (url-for "/new-registration")
               {:content-type :json
                :socket-timeout 0
                :conn-timeout 0
                :throw-exceptions false
                :accept :json
                :body (str "{\"user\":{\"personal-data\":{\"first-name\":\"Pedro\",\"last-name\":\"Santos\"},\"company-data\":{\"email\":\"donbonifacio@gmail.com\",\"phone\":\"212122121\"},\"system\":{\"password\":\"assobio\"}},\"account\":{\"company\":\"RUPEAL\",\"terms\":true,\"number-of-collaborators\":\"20\"}}")}))

(def funcs [do-login do-failed-login do-failed-new-registration])
(defn func [] (rand-nth funcs) do-login)

(do-failed-new-registration)
(let [f1 (future (dotimes [n 5000] ((func))))
      f2 (future (dotimes [n 5000] ((func))))
      f3 (future (dotimes [n 5000] ((func))))
      f4 (future (dotimes [n 5000] ((func))))
      f5 (future (dotimes [n 5000] ((func))))
      f51 (future (dotimes [n 5000] ((func))))
      f52 (future (dotimes [n 5000] ((func))))
      f53 (future (dotimes [n 5000] ((func))))
      f54 (future (dotimes [n 5000] ((func))))
      f55 (future (dotimes [n 5000] ((func))))
      f6 (future (dotimes [n 5000] ((func))))]
  (println @f51 @f53 @f53 @f54 @f55 @f1 @f2 @f3 @f4 @f5 @f6))
