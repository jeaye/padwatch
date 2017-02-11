(ns padwatch.backend
  (:require [padwatch
             [db :as db]
             [irc :as irc]]))

(defn record! [row]
  (irc/message-row! row)
  (db/insert! row))
