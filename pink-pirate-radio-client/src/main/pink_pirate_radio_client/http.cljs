(ns pink-pirate-radio-client.http
  (:require [reagent.core :as r]))


(defn programs-url 
  ([] "/entities/programs")
  ([id] (str "/entities/programs/" id)))


(defn new-program
  [done]
  (-> (js/fetch (programs-url) #js {:method "POST"
                                    :headers #js {"Content-Type" "application/json"}
                                    :body (js/JSON.stringify #js {:label "New Program"
                                                                  :data "{}"})})
      (.then #(.json %))
      (.then #(done (.-id %)))))


(defn fetch-data
  [ratom url]
  (-> (js/fetch url)
      (.then #(.json %))
      (.then #(swap! ratom assoc :state :done :data (js->clj %)))
      (.catch (fn [error]
                (js/console.log "Error while fetching from" url error)
                (swap! ratom assoc :state :error :data error)))))


(defn single-fetch-wrapper
  [url render-loading render-success render-error]
  (let [r (r/atom {:state :loading})]
    (fetch-data r url)
    (fn []
      (let [{:keys [state data]} @r]
        (cond (= :loading state) (render-loading)
              (= :done state) (render-success data) 
              (= :error state) (render-error data))))))


