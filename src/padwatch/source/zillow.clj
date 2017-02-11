(ns padwatch.source.zillow
  (:require [padwatch
             [config :as config]
             [db :as db]
             [util :as util]]
            [padwatch.source.walkscore :refer [row-walkscore]]
            [net.cgrand.enlive-html :refer [select]]
            [clojure.data.json :as json]))

; TODO: source, walkscore

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

(defn row-geo [html-data row]
  (let [matches (rest (re-matches #".*/(-?\d+\.\d+),(-?\d+\.\d+)_.*" (:url row)))
        latitude (first matches)
        longitude (second matches)]
    (if (and latitude longitude)
      (assoc row :geo (map #(Float/parseFloat %) [latitude longitude]))
      row)))

(defn row-style [html-data row]
  (let [bubble (util/select-first html-data [:div.minibubble])
        bubble-json (-> bubble :content first :data
                        (json/read-str :key-fn keyword))]
    (assoc row
           :style (format "%dBR/%.1fBa"
                          (:bed bubble-json)
                          (:bath bubble-json))
           :price (:minPrice bubble-json)
           :sqft (:sqft bubble-json))))

(defn row-where [html-data row]
  (let [addr (util/select-first html-data [:span.zsg-photo-card-address])]
    (assoc row :where (-> addr :content first))))

(defn row-title [html-data row]
  ; Zillow doesn't have good titles; use the address
  (assoc row :title (:where row)))

(defn row-dates [html-data row]
  (let [node (util/select-first html-data [:span.zsg-photo-card-notification])
        updated (-> node :content first)]
    (assoc row
           :post-data updated
           ; Zillow doesn't have this
           :available-date "N/A")))

(defn run []
  ; TODO: Have multiple zones
  (let [html-data (util/fetch-url zone-url)]
    ))
