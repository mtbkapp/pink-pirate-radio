(ns pink-pirate-radio-server.handlers
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.walk :as walk] 
            [compojure.core :as compojure :refer [context defroutes POST GET PATCH DELETE]]
            [compojure.route :as route]
            [pink-pirate-radio-server.db :as db]
            [ring.util.response :as resp]
            [pink-pirate-radio-server.deploy :as deploy])
  (:import [java.util Base64 Arrays]))


(def not-found (resp/not-found {:message "NOT FOUND!"}))


(defn handle-get-all
  [{db :db {:keys [kind]} :params}]
  (resp/response (db/list-of db kind)))


(defn handle-get-one
  [{db :db {:keys [kind id]} :params}]
  (if-let [entity (db/get-one db kind id)]
    (resp/response entity)
    not-found))


(defn handle-post
  [{db :db body :body {:keys [kind id]} :params}]
  (let [new-row (db/create db kind (walk/keywordize-keys body))]
    (resp/created (str (name kind) "/" (:id new-row)) new-row)))


(defn handle-patch
  [{db :db body :body {:keys [kind id]} :params}]
  (if-let [new-row (db/patch db kind id (walk/keywordize-keys body))]
    (resp/response new-row)
    not-found))


(defn handle-delete
  [{db :db {:keys [kind id]} :params}]
  (if (db/delete db kind id)
    {:status 204}
    not-found))


(defn handle-deploy 
  [{db :db {:keys [id]} :params}]
  (if-let [{:keys [data]} (db/get-one db :programs id)]
    (resp/response (deploy/deploy-program db (json/parse-string data))) 
    not-found))


(defn handle-stop-program
  [req]
  (deploy/stop-program)
  (resp/response {}))


(defroutes app
  (GET "/" [] (resp/redirect "/index.html"))
  (POST "/entities/programs/stop" request (handle-stop-program request))
  (POST "/entities/programs/:id/deploy" request (handle-deploy request))
  (GET "/entities/:kind" r (handle-get-all r))
  (GET "/entities/:kind/:id" r (handle-get-one r))
  (POST "/entities/:kind" r (handle-post r))
  (PATCH "/entities/:kind/:id" r (handle-patch r))
  (DELETE "/entities/:kind/:id" r (handle-delete r))
  (route/resources "/" {:root "public"})
  (route/not-found {:message "NOT FOUND"}))

