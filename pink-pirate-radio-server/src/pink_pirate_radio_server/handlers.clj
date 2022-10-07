(ns pink-pirate-radio-server.handlers
  (:require [clojure.walk :as walk] 
            [compojure.core :as compojure :refer [context defroutes POST GET PATCH DELETE]]
            [compojure.route :as route]
            [pink-pirate-radio-server.db :as db]
            [ring.util.response :as resp]
            [pink-pirate-radio-server.deploy :as deploy]))


(defn handle-get-all
  [{db :db {:keys [kind]} :params}]
  (resp/response (db/list-of db kind)))


(defn handle-get-one
  [{db :db {:keys [kind id]} :params}]
  (if-let [entity (db/get-one db kind id)]
    (resp/response entity)
    (resp/not-found nil)))


(defn handle-post
  [{db :db body :body {:keys [kind id]} :params}]
  (let [new-row (db/create db kind (walk/keywordize-keys body))]
    (resp/created (str (name kind) "/" (:id new-row)) new-row)))


(defn handle-patch
  [{db :db body :body {:keys [kind id]} :params}]
  (if-let [new-row (db/patch db kind id (walk/keywordize-keys body))]
    (resp/response new-row)
    (resp/not-found nil)))


(defn handle-delete
  [{db :db {:keys [kind id]} :params}]
  (if (db/delete db kind id)
    {:status 204}
    (resp/not-found nil)))


(defn handle-deploy 
  [{:keys [db params]}]
  (resp/response (deploy/deploy-program (:id params))))


(defroutes app
  (GET "/" [] (resp/redirect "/index.html"))
  (GET "/entities/:kind" r (handle-get-all r))
  (GET "/entities/:kind/:id" r (handle-get-one r))
  (POST "/entities/:kind" r (handle-post r))
  (PATCH "/entities/:kind/:id" r (handle-patch r))
  (DELETE "/entities/:kind/:id" r (handle-delete r))
  (POST "/deploy-program/:id" request (handle-deploy request))
  (route/resources "/" {:root "public"})
  (route/not-found {:message "NOT FOUND"}))

