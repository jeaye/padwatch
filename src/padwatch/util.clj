(ns padwatch.util
  (:require [padwatch.db :as db]
            [net.cgrand.enlive-html :refer [html-resource select]]))

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

(defn sleep [config ms]
  (println (format "[%s] sleep %sms" (:source config) ms))
  (Thread/sleep ms))

(defn skip [row-info reason]
  ; Note the ID so we can skip it, without reading, next time
  (db/insert! row-info)
  (println ("skip - " reason))
  nil)
