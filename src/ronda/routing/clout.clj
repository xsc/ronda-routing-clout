(ns ronda.routing.clout
  (:require [ronda.routing
             [descriptor :as describe]
             [generate :as gen]
             [utils :as u]]
            [clout.core :as clout]
            [clojure.string :as string]))

;; ## Compilation

(defn- assert-route
  "Assert that the given route has the right format for compilation."
  [route]
  (assert (or (string? route)
              (and (vector? route)
                   (= (count route) 2)
                   (string? (first route))
                   (map? (second route))))
          (format "route has to be either a string or a two-element vector: %s"
                  (pr-str route))))

(defn- compile-route
  "Compile the given route."
  [route]
  (assert-route route)
  (if (string? route)
    (clout/route-compile route)
    (let [[s p] route]
      (with-meta
        (clout/route-compile s p)
        {:patterns p}))))

(defn- route-patterns
  "Get patterns from a compiled route."
  [r]
  (-> r meta (:patterns {})))

;; ## Path

(let [split-char? (set "/,;?:")]
  (defn- split-route
    "Split the route to reveal potential route params."
    [route]
    (loop [sq (seq route)
           acc []
           r []]
      (if sq
        (let [c (first sq)]
          (if (and (split-char? c) (seq acc))
            (recur (next sq) [c] (conj r (apply str acc)))
            (recur (next sq) (conj acc c) r)))
        (if (seq acc)
          (conj r (apply str acc))
          r)))))

(letfn [(found? [x] (if (pos? x) x))]
  (defn- parse-route-param
    "Parse a clout route param (which might have an inline regular expression attached
     to it)."
    [^String s patterns]
    (let [a (found? (.indexOf s "{"))
          b (found? (.lastIndexOf s "}"))]
      (assert (or (and a b) (not (or a b))) "keyword with pattern looks malformed.")
      (if (and a b)
        (let [c (count s)
              k (subs s 1 a)
              p (subs s (inc a) b)]
          [[(re-pattern p) (keyword k)] (subs s (inc b))])
        (let [k (keyword (subs s 1))]
          (if-let [p (get patterns k)]
            [[p k]]
            [k]))))))

(defn- convert-route-params
  "Convert all elements starting with `:` to a keyword, attaching
   a pattern to it if desired."
  [route patterns]
  (mapcat
    (fn [^String s]
      (if (.startsWith s ":")
        (parse-route-param s patterns)
        [s]))
    route))

(defn- merge-strings
  "Merge subsequent strings into one, remove empty elements."
  [route]
  (->> route
       (partition-by string?)
       (mapcat
         (fn [[x :as v]]
           (if (string? x)
             [(apply str v)]
             v)))
       (remove #{""})))

(defn- unwrap-single-string
  "If the whole route consists of solely a string, unwrap it."
  [route]
  (if (and (not (next route))
           (string? (first route)))
    (first route)
    (vec route)))

(defn- normalize-route
  "Normalize clout route, producing a bidi-style vector."
  [route patterns]
  (-> route
      (split-route)
      (convert-route-params patterns)
      (merge-strings)
      (unwrap-single-string)))

(defn- generate-path
  "Based on a compiled route, generate a bidi-style path vector."
  [{:keys [source] :as compiled-route}]
  (normalize-route
    source
    (route-patterns compiled-route)))

;; ## Analysis

(defn- analyze*
  "Compile all routes in the given map."
  [routes]
  (reduce
    (fn [m [route-id route]]
      (let [r (compile-route route)
            p (generate-path r)]
        (-> m
            (assoc-in [:routes route-id :path] p)
            (assoc-in [:route-params route-id] (vec (:keys r)))
            (update-in [:compiled-routes] (fnil conj []) [route-id r]))))
    {} routes))

(defn- attach-existing-meta
  "Preserve metadata."
  [data existing]
  (update-in data [:routes]
             #(->> (for [[route-id m] %]
                     [route-id
                      (merge
                        (get-in existing [:routes route-id])
                        m)])
                   (into {}))))

(defn- analyze
  "Compile all routes in the given map and analyze the result."
  [routes & [existing]]
  (attach-existing-meta
    (analyze* routes)
    existing))

;; ## Match

(defn- match-route
  "Match compiled routes in-order against the given request."
  [{:keys [routes compiled-routes]} request]
  (some
    (fn [[route-id route]]
      (if-let [r (clout/route-matches route request)]
        (let [mta (get-in routes [route-id :meta])]
          (cond-> {:id route-id
                   :route-params r}
            mta (assoc :meta mta)))))
    compiled-routes))

;; ## Prefix

(defn- prefix-route
  "Create pair of `[route-string patterns]` represnting the given route
   prefixed with the given string."
  [{:keys [source] :as route} prefix additional-patterns]
  (vector
    (str prefix source)
    (merge
      (route-patterns route)
      additional-patterns)))

(defn- prefix-all-routes
  "Prefix all routes, creating pairs of `[route-string patterns]`."
  [{:keys [compiled-routes] :as data} prefix additional-patterns]
  (-> (for [[route-id route] compiled-routes]
        [route-id (prefix-route route prefix additional-patterns)])
      (analyze data)))

;; ## Descriptor

(deftype CloutDescriptor [routes]
  describe/RouteDescriptor
  (match [_ request-method path]
    (->> {:request-method request-method, :uri path}
         (match-route routes)))
  (generate [_ route-id values]
    (if-let [route-params (get-in routes [:route-params route-id])]
      (let [{:keys [path meta]} (get-in routes [:routes route-id])
            vs (u/stringify-vals values)
            rs (select-keys vs route-params)
            qs (apply dissoc vs route-params)]
        (cond-> {:path (gen/generate-by path rs)
                 :route-params rs
                 :query-params qs}
          meta (assoc :meta meta)))
      (u/throwf "unknown route ID: %s" route-id)))
  (update-metadata [_ route-id f]
    (let [v (get-in routes [:routes route-id] ::not-found)]
      (when (= v ::not-found)
        (throw
          (IllegalArgumentException.
            (format "no such route: %s" route-id))))
      (CloutDescriptor.
        (update-in routes [:routes route-id :meta] f))))
  (routes [_]
    (:routes routes))

  describe/PrefixableRouteDescriptor
  (prefix-string [_ s]
    (->> (prefix-all-routes routes s nil)
         (CloutDescriptor.)))
  (prefix-route-param [_ k pattern]
    (->> (prefix-all-routes
           routes
           (str k)
           (if pattern {k pattern}))
         (CloutDescriptor.))))

(defn descriptor
  "Generate RouteDescriptor based on a map of route ID -> clout route. Routes can
   be given as:

   - a string, e.g. `\"/articles/:id\"`
   - a vector including patterns for route params, e.g.
     `[\"/articles/:id\" {:id #\"\\d+\"}]`.
   "
  [routes]
  {:pre [(map? routes)]}
  (CloutDescriptor.
    (analyze routes)))
