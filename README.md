# ronda-routing-clout

__ronda-routing-clout__ offers a [ronda-routing](https://github.com/xsc/ronda-routing) `RouteDescriptor` for [clout](https://github.com/weavejester/clout).

[![Build Status](https://travis-ci.org/xsc/ronda-routing-clout.svg)](https://travis-ci.org/xsc/ronda-routing-clout)

## Usage

Don't. But if there really is nothing that can be done to stop you:

__Leiningen__ ([via Clojars](http://clojars.org/ronda/routing-clout))

[![Clojars Project](http://clojars.org/ronda/routing-clout/latest-version.svg)](http://clojars.org/ronda/routing-clout)

__REPL__

Use your clout route spec to generate a `RouteDescriptor`:

```clojure
(require '[ronda.routing.clout :as clout])

(def routes
  (clout/descriptor
    {:articles "/app/articles"
     :article  "/app/articles/:id"
     :home     "/app/home"}))

(def app
  (-> handler
      ;; ...
      (ronda.routing/wrap-routing routes)))
```

__Direct Usage__

```clojure
(require '[ronda.routing.descriptor :as describe])

(describe/routes routes)
;; => {:home     {:path "/app/home"},
;;     :articles {:path "/app/articles"},
;;     :article  {:path ["/app/articles/" :id]}}

(describe/match routes :get "/app/articles/123")
;; => {:id :article, :route-params {:id "123"}}

(describe/generate routes :article {:id "123", :full true})
;; => {:path "/app/articles/123",
;;     :route-params {:id "123"},
;;     :query-params {:full "true"}}
```

## License

Copyright &copy; 2015 Yannick Scherer

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
