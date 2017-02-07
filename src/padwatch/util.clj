(ns padwatch.util
  (:require [net.cgrand.enlive-html :refer [html-resource select]]
            [clojure.data.json :as json]))

(def select-first (comp first select))

(defn fetch-url [url]
  (println (str "fetch " url))
  (html-resource (java.net.URL. url)))

(defn slurp-url [url]
  (try
    (println (str "slurp " url))
    (slurp url)
    (catch Throwable _
      "{}")))

(defn sleep [ms]
  (println (str "sleep " ms "ms"))
  (Thread/sleep ms))
