(ns padwatch.source.craigslist
  (:require [padwatch
             [config :as config]
             [db :as db]
             [backend :as backend]
             [util :as util]]
            [padwatch.source.walkscore :refer [row-walkscore]]
            [net.cgrand.enlive-html :refer [select]]
            [clojure.walk :refer [postwalk]]))

(def source-config (get-in config/data [:source :craigslist]))

(defn query [params]
  (let [param-strings (map #(str (-> % first name) "=" (second %))
                           params)
        joined-params (clojure.string/join "&" param-strings)
        search-url (:search-url source-config)
        url (str search-url "?" joined-params)]
    (util/fetch-url url)))

(defn clean-tags [tags]
  (cond
    (string? tags) (clojure.string/trim tags)
    (map? tags) (apply str (:content tags))
    :else tags))

(defn select-rows [html-data]
  (select html-data [:p.result-info]))

(defn row-link [row-data row]
  (let [link (util/select-first row-data [:a.hdrlnk])
        url (str (:base-url source-config) (-> link :attrs :href))
        id (-> link :attrs :data-id)
        content (-> link :content first)]
  (assoc row
         :id (format "%s-%s" (:source row) id)
         :title content
         :url url)))

(defn row-post-date [row-data row]
  (let [post-date (-> (util/select-first row-data [:time]) :attrs :datetime)]
    (assoc row
           :post-date post-date)))

(defn row-price [row-data row]
  (let [price-str (-> (util/select-first row-data [:span.result-price])
                      :content
                      first)
        valid? (re-matches #"\$\d+" (or price-str ""))
        price (when valid?
                (Integer/parseInt (subs price-str 1)))]
    (assoc row
           :price price)))

(defn row-where [row-data row]
  (let [where (-> (util/select-first row-data [:span.result-hood]) :content first)
        trimmed (->> (or where "")
                     clojure.string/trim
                     (drop 1)
                     butlast
                     (apply str))]
    (assoc row
           :where trimmed)))

(defn row-tags [row-data row]
  (let [tags (-> (util/select-first row-data [:span.result-tags]) :content)
        cleaned (map clean-tags tags)
        useful (filter not-empty cleaned)]
    (assoc row
           :tags (flatten useful))))

(defn row-removed? [html-data]
  (util/select-first html-data [:span#has_been_removed]))

(defn row-geo [html-data row]
  (if (not (some #{"map"} (:tags row)))
    row
    (let [map-data (util/select-first html-data [:div#map])
          lat-long (when map-data
                     (map (:attrs map-data) [:data-latitude :data-longitude]))]
      (if (every? some? lat-long)
        (assoc row
               :geo lat-long)
        row))))

(defn row-available-date [html-data row]
  (let [available-date (-> (util/select-first html-data [:span.housing_movein_now])
                           :attrs
                           :data-date)]
    (assoc row
           :available-date available-date)))

(defn row-attributes [html-data row]
  (let [content (-> (util/select-first html-data [:p.attrgroup]) :content)
        cleaned (postwalk clean-tags content)
        useful (filter not-empty cleaned)
        sqft-str (second useful)
        sqft-match (re-matches #"(\d+)ft2" (or sqft-str ""))
        sqft (when sqft-match
               (Integer/parseInt (second sqft-match)))]
    (assoc row
           :style (first useful)
           :sqft sqft)))

(defn row-info [sleep-ms row-data]
  (let [link-info (row-link row-data {:source "craigslist"})]
    (when (empty (db/select {:id (:id link-info)}))
      (let [basic-extractors [row-post-date row-price
                              row-where row-tags]
            basic-info (reduce #(when %1
                                 (%2 row-data %1))
                               link-info
                               basic-extractors)
            html-data (util/fetch-url (:url basic-info))
            removed? (row-removed? html-data)]
        (when-not removed?
          (let [detailed-extractors [row-geo row-available-date
                                     row-attributes row-walkscore]
                detailed-info (reduce #(when %1
                                         (%2 html-data %1))
                                      basic-info
                                      detailed-extractors)]
            (when detailed-info
              (backend/record! detailed-info)
              (util/sleep sleep-ms)
              detailed-info)))))))

(defn total-count [html-data]
  (let [count-str  (-> (util/select-first html-data [:span.totalcount])
                       :content
                       first)]
    (Integer/parseInt (or count-str "0"))))

(defn run []
  (let [cycle-length-ms (:cycle-length-ms source-config)
        html-data (query (:params source-config))
        full-row-count (total-count html-data)
        rows (take (:max-rows source-config) (select-rows html-data))
        used-row-count (count rows)
        ; Take half as long as the cycle length to pull all rows
        row-sleep-ms (/ (/ cycle-length-ms 2) used-row-count)
        row-infos (->> (mapv (partial row-info row-sleep-ms) rows)
                       (filter some?))]
    row-infos))
