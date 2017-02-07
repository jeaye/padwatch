(ns padwatch.source.walkscore
  (:require [padwatch
             [config :as config]
             [util :as util]]
            [clojure.data.json :as json]))

(def source-config (get-in config/data [:source :walkscore]))

(defn row-walkscore [html-data row-info]
  (if-not (and (:geotag row-info)
               (:key source-config))
    row-info
    (let [lat-long (:geotag row-info)
          format-url (:format-url source-config)
          url (format format-url
                      (first lat-long)
                      (second lat-long)
                      (:key source-config))
          walk-data (json/read-str (util/slurp-url url)
                                   :key-fn keyword)]
      (when (>= (:walkscore walk-data) (:min-walkscore config/data))
        (assoc row-info
               :walkscore {:score (:walkscore walk-data)
                           :description (:description walk-data)
                           :url (:ws_link walk-data)})))))
