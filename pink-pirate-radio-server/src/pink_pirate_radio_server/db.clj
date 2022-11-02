(ns pink-pirate-radio-server.db
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs])
  (:import [java.io File]))


(defn epoch-seconds
  []
  (quot (System/currentTimeMillis) 1000))


(defn list-of
  [db kind]
  (jdbc/execute! db 
                 ["select id, updated_at, created_at, label
                  from entity where kind = ?" (name kind)]
                 {:builder-fn rs/as-unqualified-lower-maps}))


(defn get-one
  [db kind id]
  (jdbc/execute-one! db 
                     ["select * from entity where id = ? and kind = ?" id (name kind)]
                     {:builder-fn rs/as-unqualified-lower-maps}))


(defn create
  [db kind new-data]
  (let [result (sql/insert! db
                            "entity"
                            (assoc (select-keys new-data [:label :data])
                                   :kind (name kind)
                                   :created_at (epoch-seconds)
                                   :updated_at (epoch-seconds)))
        id (get result (keyword "last_insert_rowid()"))]
    (get-one db kind id)))


(defn patch
  [db kind id new-data]
  (sql/update! db
               "entity"
               (assoc (select-keys new-data [:label :data])
                      :updated_at (epoch-seconds))
               {:id id :kind (name kind)})
  (get-one db kind id))


(defn delete
  [db kind id]
  (-> (sql/delete! db "entity" {:id id :kind (name kind)})
      ::jdbc/update-count
      (= 1)))


(def song-dir (io/file (System/getProperty "user.home")
                       "music"
                       "pink-pirate-radio"))

(defn get-songs
  []
  (into []
        (comp (filter #(.isFile ^File %))
              (map (fn [^File f]
                     {:id (.getName f)
                      :label (-> (.getName f)
                                 (string/split #"\.")
                                 (first))})))
        (file-seq song-dir)))
