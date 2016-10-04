(defproject ix-stresser "1.0.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aliases {"stress-sequences" ["run"]
            "stress-basic-requests" ["run" "-m" "ix-stresser.basic-requests/-main"]
            "stress-pdfs" ["run" "-m" "ix-stresser.pdf/-main"]
            "stress-pdfnode" ["run" "-m" "ix-stresser.pdfnode/-main"]
            "stress-pdfbulknode" ["run" "-m" "ix-stresser.pdfbulknode/-main"]}
  :main ix-stresser.core
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/core.async "0.2.385"]
                 [org.clojure/data.json "0.2.6"]
                 [invoice-spec "1.3.0"]
                 [clj-http "3.2.0"]]
  :plugins [[lein-environ "1.0.3"]])
