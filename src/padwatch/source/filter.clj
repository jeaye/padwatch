(ns padwatch.source.filter
  (:require [padwatch
             [db :as db]
             [util :as util]
             [config :as config]]))

(defn within?
  ([n min] (within? n min min))
  ([n min max]
   (or (nil? n)
       (and (>= n min) (<= n max)))))

(defn unique? [row-info]
  (if (empty? (db/select {:price (:price row-info)
                          :sqft (:sqft row-info)
                          :style (:style row-info)
                          :location (:where row-info)
                          :walkscore (-> row-info :walkscore :score)}))
    row-info
    (util/skip row-info "duplicate content")))

(defn row-filter [html-data row-info]
  (if
    (and (within? (:price row-info) (:min-price config/data) (:max-price config/data))
         (within? (:sqft row-info) (:min-sqft config/data) (:max-sqft config/data))
         (within? (:bedrooms row-info) (:bedrooms config/data))
         (within? (:bathrooms row-info) (:bathrooms config/data))
         (within? (get-in row-info [:walkscore :score]) (:min-walkscore config/data) 100))
    (unique? row-info)
    (util/skip row-info "not within requirements")))
