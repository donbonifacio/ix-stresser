# InvoiceXpress stresser/verifier

You need [lein](http://leiningen.org/) installed. This script will launch
several threads that create and finalize documents on InvoiceXpress,
concurrently.

### CLI Usage

```
lein stress-sequences
lein stress-pdfs
```

To configure the required environment, you can add a `profiles.clj` file with
the following:

```clojure
{:dev {:env {:ix-ports "3001"
             :ix-server-dir "/path/to/invoicexpress"
             :ix-api-host "http://localhost"
             :ix-api-key "donbonifacio"}}
```

### Use programatically

```clojure
(core/runner {:ix-ports ["3001" "3002"]
              :ix-api-host "http://localhost"
              :ix-api-key "your-key"})
```
