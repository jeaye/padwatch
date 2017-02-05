; TODO: Rename to padwatch
(defproject clouse "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [enlive "1.1.6"]
                 [environ "1.1.0"]
                 [korma "0.4.3"]
                 [org.xerial/sqlite-jdbc "3.7.15-M1"]]
  :main ^:skip-aot clouse.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
