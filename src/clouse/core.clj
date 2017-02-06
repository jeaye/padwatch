(ns clouse.core
  (:gen-class)
  (:require [net.cgrand.enlive-html :refer [html-resource select first-child]]
            [environ.core :refer [env]]
            [clouse.db :as db]
            [clojure.pprint :refer :all]
            [clojure.walk :refer [postwalk]]
            [clojure.data.json :as json]))

; TODO:
; - amenities
; - crime?
; - pets/smoking
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
                   :sort "date"
                   :bundleDuplicates 0})

(def max-rows 5)

(def select-first (comp first select))

(defn fetch-url [url]
  (html-resource (java.net.URL. url)))

(defn query [params]
  (let [param-strings (map #(str (-> % first name) "=" (second %))
                           params)
        joined-params (clojure.string/join "&" param-strings)
        url (str search-url "?" joined-params)]
    (fetch-url url)))

(defn clean-tags [tags]
  (cond
    (string? tags) (clojure.string/trim tags)
    (map? tags) (apply str (:content tags))
    :else tags))

(defn select-rows [html-data]
  (select html-data [:p.result-info]))

(defn row-link [row-data row-info]
  (let [link (select-first row-data [:a.hdrlnk])
        url (str base-url (-> link :attrs :href))
        id (-> link :attrs :data-id)
        content (-> link :content first)]
  (assoc row-info
         :source "craigslist"
         :id id
         :title content
         :url url)))

(defn row-post-date [row-data row-info]
  (let [post-date (-> (select-first row-data [:time]) :attrs :datetime)]
    (assoc row-info
           :post-date post-date)))

(defn row-price [row-data row-info]
  (let [price-str (-> (select-first row-data [:span.result-price])
                      :content
                      first)
        valid? (re-matches #"\$\d+" (or price-str ""))
        price (when valid?
                (Integer/parseInt (subs price-str 1)))]
    (assoc row-info
           :price price)))

(defn row-where [row-data row-info]
  (let [where (-> (select-first row-data [:span.result-hood]) :content first)
        trimmed (->> (or where "")
                     clojure.string/trim
                     (drop 1)
                     butlast
                     (apply str))]
    (assoc row-info
           :where trimmed)))

(defn row-tags [row-data row-info]
  (let [tags (-> (select-first row-data [:span.result-tags]) :content)
        cleaned (map clean-tags tags)
        useful (filter not-empty cleaned)]
    (assoc row-info
           :tags (flatten useful))))

(defn row-removed? [html-data]
  (select-first html-data [:span#has_been_removed]))

(defn row-geotag [html-data row-info]
  (if (not (some #{"map"} (:tags row-info)))
    row-info
    (let [map-data (select-first html-data [:div#map])
          lat-long (when map-data
                     (map (:attrs map-data) [:data-latitude :data-longitude]))]
      (if (every? some? lat-long)
        (assoc row-info
               :geotag lat-long)
        row-info))))

(defn row-available-date [html-data row-info]
  (let [available-date (-> (select-first html-data [:span.housing_movein_now])
                           :attrs
                           :data-date)]
    (assoc row-info
           :available-date available-date)))

(defn row-attributes [html-data row-info]
  (let [content (-> (select-first html-data [:p.attrgroup]) :content)
        cleaned (postwalk clean-tags content)
        useful (filter not-empty cleaned)
        sqft-str (second useful)
        sqft-match (re-matches #"(\d+)ft2" (or sqft-str ""))
        sqft (when sqft-match
               (Integer/parseInt (second sqft-match)))]
    (assoc row-info
           :style (first useful)
           :sqft sqft)))

(defn row-walk-score [html-data row-info]
  (if-not (or (:geotag row-info)
              (env :walkscore-key))
    row-info
    (let [lat-long (:geotag row-info)
          base-url "http://api.walkscore.com/score?format=json&lat=%s&lon=%s&wsapikey=%s"
          url (format base-url
                      (first lat-long)
                      (second lat-long)
                      (env :walkscore-key))
          walk-data (json/read-str (slurp url)
                                   :key-fn keyword)]
      (assoc row-info
             :walkscore {:score (:walkscore walk-data)
                         :description (:description walk-data)
                         :url (:ws_link walk-data)}))))

(defn row-info [row-data]
  (let [link-info (row-link row-data {})]
    (when (empty? (db/select {:id (:id link-info)}))
      (let [basic-extractors [row-post-date row-price
                              row-where row-tags]
            basic-info (reduce #(%2 row-data %1)
                               link-info
                               basic-extractors)
            html-data (fetch-url (:url basic-info))
            removed? (row-removed? html-data)]
        (when-not removed?
          (let [detailed-extractors [row-geotag row-available-date
                                     row-attributes row-walk-score]
                detailed-info (reduce #(%2 html-data %1)
                                      basic-info
                                      detailed-extractors)]
            detailed-info))))))

(defn total-count [html-data]
  (let [count-str  (-> (select-first html-data [:span.totalcount])
                       :content
                       first)]
    (Integer/parseInt (or count-str "0"))))

(defn -main [& args]
  (db/create!)
  (let [html-data (query query-params)
        rows (take max-rows (select-rows data))
        row-infos (filter some? (map row-info rows))]
    (doseq [row row-infos]
      (db/insert! row))
    (pprint row-infos)))
