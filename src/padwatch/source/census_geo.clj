(ns padwatch.source.census-geo
  (:require [padwatch
             [config :as config]
             [util :as util]]
            [net.cgrand.enlive-html :refer [select text-pred]]
            [clojure.data.json :as json]))

(def source-config (get-in config/data [:source :census-geo]))

(defn row-census-geo [html-data row]
  (let [address (:where row)
        sanitized-address (-> (clojure.string/replace address #"\s+" "+")
                              (clojure.string/replace #"," ""))
        format-url (:format-url source-config)
        url (format format-url sanitized-address)
        html-data (util/fetch-url url)
        results (util/select-first html-data
                                   [:div#pl_gov_census_geo_geocoder_domain_AddressResult])
        coord-line (util/select-first results
                                      [(text-pred #(re-matches #".*Coordinates:.*" %))])
        coord-matches (-> (re-matches #".*X:\s*(-?\d+\.\d+)\s*Y:\s*(-?\d+\.\d+).*"
                                      (or coord-line ""))
                          rest)]
    (if (not-empty coord-matches)
      (assoc row :geo (map #(Float/parseFloat %) (reverse coord-matches)))
      row)))
