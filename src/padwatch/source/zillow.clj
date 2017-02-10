(ns padwatch.source.zillow
  (:require [padwatch
             [config :as config]
             [db :as db]
             [util :as util]]
            [padwatch.source.walkscore :refer [row-walkscore]]
            [net.cgrand.enlive-html :refer [select]]
            [clojure.data.json :as json]))

(def source-config (get-in config/data [:source :zillow]))

(def url "http://www.zillow.com/burlingame-ca/apartments/")

(defn rows [html-data]
  (-> (util/select-first html-data [:ul.photo-cards])
      :content))

(defn row-info [row-data]
  (let [bubble (util/select-first row-data [:div.minibubble])
        bubble-json (-> bubble :content first :data
                        (json/read-str :key-fn keyword))]
      {:style (format "%dBR/%.1fBa"
                      (:bed bubble-json)
                      (:bath bubble-json))
       :price (:minPrice bubble-json)
       :sqft (:sqft bubble-json)}))

(defn run []
  (let [html-data (util/fetch-url url)]
    ))
