(defproject padwatch "0.1.0-SNAPSHOT"
  :description "A continuous apartment listing watcher"
  :url "https://github.com/jeaye/padwatch"
  :license {:name "jank license"
            :url "https://upload.jeaye.com/jank-license"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [enlive "1.1.6"]
                 [environ "1.1.0"]
                 [korma "0.4.3"]
                 [org.xerial/sqlite-jdbc "3.7.15-M1"]
                 [irclj "0.5.0-alpha4"]]
  :main ^:skip-aot padwatch.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
