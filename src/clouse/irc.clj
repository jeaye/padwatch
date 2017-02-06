(ns clouse.irc
  (:require [irclj.core :as irc]))

(def nick "padwatch")
(def channel "#padwatch")

(def connection (atom nil))

(defn eat-log [& args]
  (comment pprint args))

(defn disconnect! []
  (when @connection
    (swap! connection irc/kill)))

(defn connect! []
  (disconnect!)
  (reset! connection (irc/connect "irc.freenode.net"
                                  6667 nick
                                  :callbacks {:raw-log eat-log}))
  (irc/join @connection channel))

(defn shorten-url [url]
  (when url
    (slurp (str "http://tinyurl.com/api-create.php?url=" url))))

(defn message! [row-info]
  (let [useful {:title (:title row-info) ; TODO: extract helper
                :where (:where row-info)
                :style (:style row-info)
                :sqft (:sqft row-info)
                :url (-> row-info :url shorten-url)
                :walkscore (update (:walkscore row-info)
                                   :url shorten-url)}]
    (irc/message @connection channel (pr-str useful))))
