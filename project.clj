(defproject ronda/routing-clout "0.1.1-SNAPSHOT"
  :description "ronda RouteDescriptor for clout."
  :url "https://github.com/xsc/ronda-routing-clout"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"
            :year 2015
            :key "mit"}
  :dependencies [[org.clojure/clojure "1.7.0-beta2"]
                 [ronda/routing "0.2.7"]
                 [clout "2.1.1"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [joda-time "2.7"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :aliases {"test" ["midje"]}
  :pedantic? :abort)
