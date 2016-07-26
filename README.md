# InvoiceXpress stresser/verifier

You need [lein](http://leiningen.org/) installed. This script will launch
several threads that create and finalize documents on InvoiceXpress,
concurrently.

```clojure
(core/runner {:ix-ports ["3001" "3002"]
              :ix-api-host "http://localhost"
              :ix-api-key "your-key"})
```
