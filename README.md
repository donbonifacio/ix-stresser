# InvoiceXpress stresser/verifier

You need [lein](http://leiningen.org/) installed. This script will launch
several threads that create and finalize documents on InvoiceXpress,
concurrently.

Edit `core_test.clj` to change the defaults:

```clojure
(def threads 5)
(def docs-per-thread 200)
```

Edit `core.clj` to change the default connection:

```clojure
(def url "http://donbonifacio.bizflow-makeover.com")
(def ix-api-key "****")
(def ports [3001 3002])
```

The script will randomly iterate for each given port.

Run with:

```
lein test
```
