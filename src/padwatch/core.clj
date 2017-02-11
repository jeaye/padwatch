(ns padwatch.core
  (:gen-class)
  (:require [padwatch
             [irc :as irc]
             [db :as db]
             [config :as config]
             [util :as util]]
            [padwatch.source
             [craigslist :as craigslist]
             [zillow :as zillow]]
            [clojure.pprint :refer [pprint]]))

; TODO:
; - amenities
; - crime?
; - pets/smoking
; - allow useful query params

(defn -main [& args]
  (db/create!)
  (irc/connect!)
  (loop []
    (println "scrape")
    (try
      (let [craigslist-rows (craigslist/run)]
        (irc/message! (str "Added " (count craigslist-rows) " listings; "
                           "db has " (db/total-count) " total.")))
      (catch Throwable t ; Just keep trying
        (println t)))
    (recur)))
