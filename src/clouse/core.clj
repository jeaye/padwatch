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

(defn select-rows [html-data]
  (html/select html-data [:p.result-info]))

(defn row-info [row]
  (let [link (first (html/select row [:a.hdrlnk]))
        id (-> link :attrs :data-id)
        content (:content link)
        url (str base-url (-> link :attrs :href))]
    {:id id
     :content content
     :url url}))

(defn -main
  [& args]
  (pprint (query {:min_price 1500
                  :max_price 3000})))
