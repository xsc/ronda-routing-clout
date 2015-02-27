(defproject ronda/routing-clout "0.1.0-SNAPSHOT"
  :description "ronda RouteDescriptor for clout."
  :url "https://github.com/xsc/ronda-routing-clout"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha5"]
                 [ronda/routing "0.1.0"]
                 [clout "2.1.0"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [joda-time "2.7"]]
                   :plugins [[lein-midje "3.1.3"]]}}
  :aliases {"test" ["midje"]}
  :pedantic? :abort)
