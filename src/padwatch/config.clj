(ns padwatch.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def data (-> (io/resource "config.edn")
              slurp
              edn/read-string
              eval))
