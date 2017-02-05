(ns clouse.core
  (:gen-class)
  (:require [net.cgrand.enlive-html :as html]
            [clojure.pprint :refer :all]))

(def area "sfbay")
(def section "apa")
(def base-url (format "https://%s.craigslist.org"
                      area))
(def search-url (format "%s/search/%s"
                        base-url
                        section))

(defn fetch-url [url]
  (html/html-resource (java.net.URL. url)))

(defn query [params]
  (let [param-strings (map #(str (-> % first name) "=" (second %))
                           params)
        joined-params (clojure.string/join "&" param-strings)
        url (str search-url "?" joined-params)]
    (fetch-url url)))

(defn select-rows [html-data]
  (html/select html-data [:p.result-info]))

(defn row-link [row-data row-info]
  (let [link (first (html/select row-data [:a.hdrlnk]))
        url (str base-url (-> link :attrs :href))
        id (-> link :attrs :data-id)
        content (-> link :content first)]
  (assoc row-info
         :id id
         :content content
         :url url)))

(defn row-date [row-data row-info]
  (let [date (-> (html/select row-data [:time]) first :attrs :datetime)]
    (assoc row-info
           :date date)))

(defn row-price [row-data row-info]
  (let [price-str (-> (html/select row-data [:span.result-price])
                      first
                      :content
                      first)
        valid? (re-matches #"\$\d+" (or price-str ""))
        price (when valid?
                (Integer/parseInt (subs price-str 1)))]
    (assoc row-info
           :price price)))

(defn row-where [row-data row-info]
  (let [where (-> (html/select row-data [:span.result-hood]) first :content first)
        trimmed (->> where
                     clojure.string/trim
                     (drop 1)
                     butlast
                     (apply str))]
    (assoc row-info
           :where trimmed)))

(defn row-tags [row-data row-info]
  (let [tags (-> (html/select row-data [:span.result-tags]) first :content)
        clean #(cond
                 (string? %) (clojure.string/trim %)
                 (map? %) (:content %)
                 :else %)
        cleaned (map clean tags)
        useful (filter not-empty cleaned)]
    (assoc row-info
           :tags (flatten useful))))

(defn row-info [row-data]
  (let [extractors [row-link row-date row-price row-where row-tags]
        info (reduce #(%2 row-data %1) {} extractors)]
    info))

(defn -main
  [& args]
  (pprint (query {:min_price 1500
                  :max_price 3000})))
