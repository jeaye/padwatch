(ns clouse.core
  (:gen-class)
  (:require [net.cgrand.enlive-html :as html]
            [clojure.pprint :refer :all]))

; TODO:
; - sqft and amenities
; - date available (Mar 1st onward)
; - walkscore
; - crime?
; - pets/smoking
; - neighborhoods? -- nope
; - points of interest -- nope
; - transit? time to work? -- nope
; - allow useful query params

(def area "sfbay")
(def section "apa")
(def base-url (format "https://%s.craigslist.org"
                      area))
(def search-url (format "%s/search/%s"
                        base-url
                        section))

(def query-params {:min_price 1500
                   :max_price 3000
                   :hasPic 1
                   :minSqft 600
                   :maxSqft 1000
                   :no_smoking 1
                   :availabilityMode 0 ; All dates
                   :bedrooms 1
                   :bathrooms 1
                   :sort "date"})

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

(defn row-post-date [row-data row-info]
  (let [post-date (-> (html/select row-data [:time]) first :attrs :datetime)]
    (assoc row-info
           :post-date post-date)))

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
        trimmed (->> (or where "")
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

(defn row-geotag [html-data row-info]
  (if (not (some #{"map"} (:tags row-info)))
    row-info
    (let [map-data (first (html/select html-data [:div#map]))
          lat-long (when map-data
                     (map (:attrs map-data) [:data-latitude :data-longitude]))]
      (assoc row-info
             :geotag lat-long))))

(defn row-available-date [html-data row-info]
  (let [available-date (-> (html/select html-data [:span.housing_movein_now])
                           first
                           :attrs
                           :data-date)]
    (assoc row-info
           :available-date available-date)))

(defn row-info [row-data]
  (let [basic-extractors [row-link row-post-date row-price row-where row-tags]
        basic-info (reduce #(%2 row-data %1) {} basic-extractors)
        ; TODO: Check if it's a new entry; bail otherwise
        html-data (fetch-url (:url basic-info))
        detailed-extractors [row-geotag row-available-date]
        detailed-info (reduce #(%2 html-data %1) basic-info detailed-extractors)]
    detailed-info))

(defn total-count [html-data]
  (let [count-str  (-> (html/select html-data [:span.totalcount])
                       first
                       :content
                       first)]
    (Integer/parseInt (or count-str "0"))))

(defn -main
  [& args]
  (pprint (query query-params)))
