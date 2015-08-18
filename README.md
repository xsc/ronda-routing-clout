# ronda-routing-clout

__ronda-routing-clout__ offers a [ronda-routing](https://github.com/xsc/ronda-routing) `RouteDescriptor` for [clout](https://github.com/weavejester/clout),
actually implementing bidirectionality (i.e. path generation) on top of it.

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

```
The MIT License (MIT)

Copyright (c) 2015 Yannick Scherer

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
