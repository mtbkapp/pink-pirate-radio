(ns pink-pirate-radio-client.main 
  (:require [clojure.string :as string]
            [goog.events :as events]
            [pink-pirate-radio-client.programmer :as programmer]
            [pink-pirate-radio-client.http :as http]
            [reagent.core :as r] 
            [reagent.dom :as rdom]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:import [goog History]
           [goog.history EventType]))


(defn start-routing!
  []
  (secretary/set-config! :prefix "#")
  (doto (History.)
    (events/listen EventType.NAVIGATE
                   #(secretary/dispatch! (.-token ^object %)))
    (.setEnabled true)))


(def view (r/atom :home))

(defn goto!
  [& url-segments]
  (.assign js/window.location (str "#/" (string/join "/" url-segments))))


(defroute home-route "/" []
  (reset! view [:home]))

(defroute edit-program-route "/programs/:id" [id]
  (reset! view [:edit-program id]))


(defn on-new-program-click
  []
  (http/new-program (fn [new-program-id] (goto! "programs" new-program-id))))


(defn list-programs-view
  [programs]
  [:div {:id "programs-list"}
   [:div 
    [:h2 "Programs"]
    [:button {:type "button" :on-click on-new-program-click} "New Program"]
    (into [:ul]
          (map (fn [{:strs [id label] :as p}]
                 [:li [:a {:href (str "#/programs/" id)} label]]))
          (sort-by (comp - #(get % "updated_at")) programs))]])


(defn list-programs
  []
  (http/single-fetch-wrapper
    (http/programs-url) 
    (fn []
      [:div "loading programs"])
    (fn [programs]
      [list-programs-view programs])
    (fn [error]
      [:div "Error loading programs"])))


(defn root 
  []
  (let [[view-name & args] @view]
    (case view-name
      :home [list-programs]
      :edit-program [:div "edit" (prn-str args)]
      )))


(defn render-app!
  []
  (rdom/render [root] (js/document.getElementById "app")))


(defn init 
  []
  (start-routing!)
  (render-app!))

