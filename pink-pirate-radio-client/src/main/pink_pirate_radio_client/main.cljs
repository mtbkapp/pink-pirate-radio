(ns pink-pirate-radio-client.main 
  (:require [goog.events :as events]
            [pink-pirate-radio-client.programmer :as programmer]
            [pink-pirate-radio-client.http :as http]
            [pink-pirate-radio-client.recorder :as recorder]
            [pink-pirate-radio-client.utils :as utils]
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


(defroute home-route "/" []
  (reset! view [:home]))

(defroute edit-program-route "/programs/:id" [id]
  (reset! view [:edit-program id]))

(defroute sounds-view-route "/sounds" []
  (reset! view [:sounds]))


(defn on-new-program-click
  []
  (http/new-program (fn [new-program-id] (utils/goto! "programs" new-program-id))))


(defn list-programs-view
  [{{state :state programs :data} :programs}]
  [:div
   [:h2 "Programs"]
   (cond (= :loading state) 
         [:div "Loading"]
         (= :error state)
         [:div "Error"]
         :else 
         [:div {:id "programs-list"}
          [:button {:type "button" :on-click on-new-program-click} "New Program"]
          (into [:ul]
                (map (fn [{:strs [id label] :as p}]
                       [:li [:a {:href (str "#/programs/" id)} label]]))
                (sort-by (comp - #(get % "updated_at")) programs))])])


(defn list-programs
  []
  (http/fetch-wrapper
    {:programs (http/entity-url :programs)}
    list-programs-view))



(defn sounds-link 
  []
  [:h2 [:a {:href "#/sounds"} "Sounds"]])


(defn root 
  []
  (let [[view-name & args] @view]
    (case view-name
      :home [:div [list-programs] [sounds-link]]
      :edit-program [programmer/editor (first args)]
      :sounds [recorder/editor])))


(defn render-app!
  []
  (rdom/render [root] (js/document.getElementById "app")))


(defn init 
  []
  (start-routing!)
  (render-app!))

