(ns ronda.routing.bidi-test
  (:require [midje.sweet :refer :all]
            [ronda.routing
             [bidi :as bidi]
             [descriptor :as describe]
             [prefix :as p]]))

(def digit #"\d+")

(tabular
  (fact "about bidi route listing."
        (let [d (bidi/descriptor ?routes)
              r (describe/routes d)]
          (into {} (map (juxt key (comp :path val)) r)) => ?list
          (map (comp :methods val) r) => (has every? empty?)
          ))
  ?routes, ?list
  ["/" :endpoint]
  {:endpoint "/"}

  ["/" {"a" :a, "b" :b}]
  {:a "/a", :b "/b"}

  [["/" :id] :endpoint]
  {:endpoint ["/" :id]}

  [["/" [digit :id]] :endpoint]
  {:endpoint ["/" [digit :id]]}

  ["/" {["a/" :id] :a
        ["b/" :id] {"" :b
                    "/c" :c}}]
  {:a ["/a/" :id]
   :b ["/b/" :id]
   :c ["/b/" :id "/c"]})

(fact "about bidi route listing + methods."
      (describe/routes
        (bidi/descriptor
          ["/" {"article" {:get :article, :put :save-article}}]))
      => {:article {:path "/article", :methods #{:get}}
          :save-article {:path "/article", :methods #{:put}}})

(let [d (bidi/descriptor
          ["/" {["a/" :id] :a
                "b/" {[:id] :b
                      [:id "/" :action] :c}}])]
  (tabular
    (fact "about bidi route matching."
          (let [r (describe/match d :get ?uri)]
            (:id r) => ?id
            (:route-params r) => ?route-params))
    ?uri              ?id      ?route-params
    "/a/id"           :a       {:id "id"}
    "/b/id"           :b       {:id "id"}
    "/b/id/go"        :c       {:id "id" :action "go"}
    "/unknown"        nil      nil)
  (tabular
    (fact "about bidi route generation."
          (let [r (describe/generate d ?id ?values)]
            (:path r) => ?path
            (:route-params r) => ?route-params
            (:query-params r) => ?query-params))
    ?id ?values                  ?path       ?route-params            ?query-params
    :a  {:id "id"}               "/a/id"     {:id "id"}               {}
    :a  {:id "id", :c 0}         "/a/id"     {:id "id"}               {:c "0"}
    :c  {:id "id", :action "go"} "/b/id/go"  {:id "id", :action "go"} {})
  (fact "about exceptions."
        (describe/generate d :unknown {})
        => (throws Exception #"unknown route ID")

        (describe/generate
          (bidi/descriptor [["/" [digit :id]] :x])
          :x {:id "abc"})
        => (throws Exception #"not compatible"))
  (let [route-prefix ["/" [#"de|fr|us" :locale]]
        route-path "/de/b/id/go"
        route-params {:locale "de", :id "id", :action "go"}
        d' (p/prefix d route-prefix)]
    (tabular
      (fact "about prefixed routes."
            (-> (describe/routes d') ?endpoint :path)
            => (reduce conj route-prefix ?suffix))
      ?endpoint ?suffix
      :a        ["/a/" :id]
      :b        ["/b/" :id]
      :c        ["/b/" :id "/" :action])
    (fact "about matching prefixed routes."
          (describe/match d' :get "/a/id") => nil?
          (describe/match d' :get route-path)
          => {:id :c
              :route-params route-params})
    (fact "about generating prefixed routes."
          (describe/generate d' :d route-params)
          => (throws Exception #"unknown route ID")
          (describe/generate d' :c (assoc route-params :locale "nl"))
          => (throws Exception #"not compatible")
          (describe/generate d' :c route-params)
          => {:path         route-path
              :route-params route-params
              :query-params {}}
          (describe/generate d' :a route-params)
          => {:path         "/de/a/id"
              :route-params (dissoc route-params :action)
              :query-params {:action "go"}})))
