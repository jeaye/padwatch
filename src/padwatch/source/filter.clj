(ns padwatch.source.filter
  (:require [padwatch
             [config :as config]]))

(defn within?
  ([n min] (within? n min min))
  ([n min max]
   (or (nil? n)
       (and (>= n min) (<= n max)))))

(defn row-filter [html-data row]
  (when
    (and (within? (:price row) (:min-price config/data) (:max-price config/data))
         (within? (:sqft row) (:min-sqft config/data) (:max-sqft config/data))
         (within? (:bedrooms row) (:bedrooms config/data))
         (within? (:bathrooms row) (:bathrooms config/data))
         (within? (get-in row [:walkscore :score]) (:min-walkscore config/data) 100))
    row))
