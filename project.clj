(defproject uk.org.1729/movielens "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clojurewerkz/titanium "1.0.0-beta2-SNAPSHOT"]
                 [clj-time "0.8.0"]
                 [com.thinkaurelius.titan/titan-core "0.5.0"]
                 [com.thinkaurelius.titan/titan-berkeleyje "0.5.0"]
                 [com.thinkaurelius.titan/titan-lucene "0.5.0"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.trace "0.7.8"]]}})
