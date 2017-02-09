(ns padwatch.source.zillow
  (:require [padwatch
             [config :as config]
             [db :as db]
             [util :as util]]
            [padwatch.source.walkscore :refer [row-walkscore]]
            [net.cgrand.enlive-html :refer [select]]))
