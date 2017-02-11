(ns padwatch.backend
  (:require [padwatch
             [db :as db]
             [irc :as irc]]
            [clojure.pprint :refer [pprint]]))

(defn record! [row]
  (irc/message-row! row)
  (db/insert! row)
  (comment pprint row))
