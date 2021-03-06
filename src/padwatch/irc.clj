(ns padwatch.irc
  (:require [padwatch.util :as util]
            [irclj.core :as irc]))

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

(defn message! [msg]
  (irc/message @connection channel msg))

(defn message-row! [row-info]
  (let [useful (merge (util/extract row-info [:where :style :price :sqft])
                      {:url (-> row-info :url shorten-url)
                       :walkscore (-> (:walkscore row-info)
                                      (update :url shorten-url)
                                      (dissoc :description))})]
    (irc/message @connection channel (pr-str useful))))
