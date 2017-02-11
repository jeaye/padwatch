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

(defn -main [& args]
  (db/create!)
  (irc/connect!)
  (let [sources (map #(future (%)) [craigslist/run zillow/run])]
    ; Block forever
    (mapv deref sources)))
