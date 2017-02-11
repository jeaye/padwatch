(ns padwatch.source.zillow
  (:require [padwatch
             [config :as config]
             [db :as db]
             [irc :as irc] ; TODO: remove
             [util :as util]]
            [padwatch.source
             [walkscore :refer [row-walkscore]]
             [census-geo :refer [row-census-geo]]]
            [net.cgrand.enlive-html :refer [select]]
            [clojure.data.json :as json]))

(def source-config (get-in config/data [:source :zillow]))

(def base-url "http://www.zillow.com")
(def zone-url (str base-url "/burlingame-ca/apartments"))

(defn rows [html-data]
  (-> (util/select-first html-data [:ul.photo-cards])
      :content))

(defn row-id [html-data row]
  (let [id (-> html-data :content first :attrs :data-zpid)]
    (assoc row :id (format "%s-%s" (:source row) id))))

(defn row-url [html-data row]
  (let [anchor (util/select-first html-data [:a.zsg-photo-card-overlay-link])
        href (-> anchor :attrs :href)]
    (assoc row :url (str base-url href))))

(defn row-style [html-data row]
  (let [bubble (util/select-first html-data [:div.minibubble])
        bubble-data (-> bubble :content first :data)
        bubble-json (json/read-str (or bubble-data "{}") :key-fn keyword)]
    (when (not-empty bubble-json)
      (assoc row
             :style (format "%dBR/%.1fBa"
                            (:bed bubble-json)
                            (:bath bubble-json))
             :price (:minPrice bubble-json)
             :sqft (:sqft bubble-json)))))

(defn row-where [html-data row]
  (let [addr (util/select-first html-data [:span.zsg-photo-card-address])]
    (assoc row :where (-> addr :content first))))

(defn row-title [html-data row]
  ; Zillow doesn't have good titles; use the address
  (assoc row :title (:where row)))

(defn row-dates [html-data row]
  (let [node (util/select-first html-data [:span.zsg-photo-card-notification])
        updated (-> node :content first)]
    ; Will be "Updated today/yesterday" or a map with the number of days. Ignore
    ; anything that old.
    (when (string? updated)
      (assoc row
             :post-date updated
             ; Zillow doesn't have this
             :available-date "N/A"))))

(defn row-info [row-data]
  (let [row (row-id row-data {:source "zillow"})]
    (when (empty (db/select {:id (:id row)}))
      (let [extractors [row-url
                        row-style row-where
                        row-title row-dates
                        row-census-geo row-walkscore]
            final-row (reduce #(when %1
                                 (%2 row-data %1))
                              row
                              extractors)]
        (when final-row
          ; TODO: (backend/record! detailed-info)
          ;(irc/message-row! final-row)
          ;(db/insert! final-row)
          final-row)))))

(defn run []
  ; TODO: Have multiple zones
  (let [html-data (util/fetch-url zone-url)
        row-data (rows html-data)
        row-infos (filter some? (mapv row-info row-data))]
    row-infos))
