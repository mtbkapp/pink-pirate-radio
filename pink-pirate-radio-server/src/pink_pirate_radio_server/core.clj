(ns pink-pirate-radio-server.core
  (:require [com.stuartsierra.component :as component]
            [pink-pirate-radio-server.handlers :as handlers]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :as ring-defaults]
            [ring.middleware.json :as ring-json]
            [next.jdbc :as jdbc])
  (:import [liquibase Contexts Liquibase]
           [liquibase.database DatabaseFactory]
           [liquibase.database.jvm JdbcConnection]
           [liquibase.resource ClassLoaderResourceAccessor])
  (:gen-class))


(defn db-middleware
  [handler db]
  (fn [req]
    (with-open [conn (jdbc/get-connection db)]
      (handler (assoc req :db conn)))))


(defrecord HttpServer
  [host port ssl-port server db]
  component/Lifecycle 
  (start [this]
    (assoc this :server (jetty/run-jetty 
                          (-> handlers/app
                              (db-middleware db)
                              (ring-json/wrap-json-response)
                              (ring-json/wrap-json-body))
                          {:host host 
                           :port port 
                           :ssl-port ssl-port 
                           :keystore "keystore.jks"
                           :key-password "password"
                           :join? false})))
  (stop [this]
    (.stop server)
    (assoc this :server nil)))


(def changelog-path "migrations/changelog.xml")

(defn migrate-db
  [conn]
  (let [db (.findCorrectDatabaseImplementation (DatabaseFactory/getInstance) 
                                               (JdbcConnection. conn))
        lqb (Liquibase. ^String changelog-path
                        (ClassLoaderResourceAccessor.)
                        db)]
    (.update lqb (Contexts.))))

(defrecord SqliteDb
  [jdbcUrl]
  component/Lifecycle 
  (start [this]
    ; migrate db
    (with-open [conn (jdbc/get-connection this)]
      (migrate-db conn))
    this)
  (stop [this]
    this))


(defn system
  [host port ssl-port db-file]
  (component/system-map :db (map->SqliteDb {:jdbcUrl (str "jdbc:sqlite:" db-file)})
                        :server (component/using
                                  (map->HttpServer {:host host :port port :ssl-port ssl-port})
                                  {:db :db})))


(defn -main
  [& args]
  (component/start (system "0.0.0.0" 9875 9876 "data.sqlite3")))


(comment
  (do
    (component/stop s)
    (def s (-main)))
  (keys s)
  (prn (:server s))
  (component/stop s)
  
  )
