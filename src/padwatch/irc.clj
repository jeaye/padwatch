(ns padwatch.irc
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
    (try
      (slurp (str "http://tinyurl.com/api-create.php?url=" url))
      (catch Throwable _ ; tinyurl can time out; just skip the shortening
        url))))

(defn message! [row-info]
  ; TODO: extract helper
  (let [useful {:where (:where row-info)
                :style (:style row-info)
                :sqft (:sqft row-info)
                :url (-> row-info :url shorten-url)
                :walkscore (-> (:walkscore row-info)
                               (update :url shorten-url)
                               (dissoc :description))
                }]
    (irc/message @connection channel (pr-str useful))))
