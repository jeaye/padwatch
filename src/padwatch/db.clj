(ns padwatch.db
  (:require [korma
             [core :as sql]
             [db :as db]]))

(db/defdb sqlite3-db (db/sqlite3 {}))

(sql/defentity listings
  (sql/pk :id)
  (sql/entity-fields :id :price :sqft :style :walkscore :location :url :source))

(defn create! []
  (sql/exec-raw sqlite3-db
                (str "create table if not exists listings ("
                     "id text,"
                     "price integer,"
                     "sqft integer,"
                     "style text,"
                     "walkscore integer,"
                     "location text,"
                     "url text,"
                     "source text"
                     ")")
                :keys))

(defn insert! [row-info]
  (println (str "insert " (:url row-info)))
  (sql/insert listings
              (sql/values {:id (:id row-info)
                           :price (:price row-info)
                           :sqft (:sqft row-info)
                           :style (:style row-info)
                           :walkscore (-> row-info :walkscore :score)
                           :location (:where row-info)
                           :url (:url row-info)
                           :source (:source row-info)})))

(defn select [where]
  (sql/select listings (sql/where where)))
