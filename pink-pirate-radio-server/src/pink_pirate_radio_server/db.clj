(ns pink-pirate-radio-server.db
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]))

; 
; (defn get-all
;   [db table]
;   (jdbc/execute! db
;                  [(str "select * from " (name table))]
;                  {:builder-fn rs/as-unqualified-lower-maps}))
; 
; 
; (defn get-one
;   [db table id]
;   (jdbc/execute-one! db 
;                      [(str "select * from " (name table) " where id = ?") id]
;                      {:builder-fn rs/as-unqualified-lower-maps}))
; 
; 
; (defn create
;   [db table data]
;   (let [id (-> (sql/insert! db
;                             table
;                             (-> data
;                                 (dissoc :id)
;                                 (assoc :updated_at (System/currentTimeMillis)
;                                        :created_at (System/currentTimeMillis))))
; 
;                (get (keyword "last_insert_rowid()")))]
;     (get-one db table id)))
; 
; 
; (defn patch
;   [db table id data]
;   (sql/update! db 
;                table 
;                (-> data 
;                    (dissoc :id :created_at)
;                    (assoc :updated_at (System/currentTimeMillis)))
;                {:id id})
;   (get-one db table id))
; 
; 
; (defn delete 
;   [db table id]
;   (-> (sql/delete! db table {:id id})
;       ::jdbc/update-count
;       (= 1)))
; 

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
