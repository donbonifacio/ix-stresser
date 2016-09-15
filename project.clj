(defproject ix-stresser "1.0.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aliases {"stress-sequences" ["run"]
            "stress-pdfs" ["run" "-m" "ix-stresser.pdf/-main"]}
  :main ix-stresser.core
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/core.async "0.2.385"]
                 [invoice-spec "1.3.0"]]
  :plugins [[lein-environ "1.0.3"]])
