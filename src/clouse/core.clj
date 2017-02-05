(ns clouse.core
  (:gen-class)
  (:require [net.cgrand.enlive-html :as html]
            [clojure.pprint :refer :all]))

(def area "sfbay")
(def section "apa")
(def base-url (format "https://%s.craigslist.org/search/%s/"
                      area
                      section))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn query [params]
  (let [param-strings (map #(str (-> % first name) "=" (second %))
                           params)
        joined-params (clojure.string/join "&" param-strings)
        url (str base-url "?" joined-params)]
    (fetch-url url)))

(defn -main
  [& args]
  (pprint (query {:min_price 1500
                  :max_price 3000})))
