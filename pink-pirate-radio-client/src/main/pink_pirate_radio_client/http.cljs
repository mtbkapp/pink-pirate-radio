(ns pink-pirate-radio-client.http
  (:require [clojure.core.async :as async]
            [pink-pirate-radio-client.utils :as utils]
            [reagent.core :as r]))


(defn entity-url
  ([kind] (str "/entities/" (name kind)))
  ([kind id] (str "/entities/" (name kind) "/" id))
  ([kind id action] (str "/entities/" (name kind) "/" id "/" (name action))))


(defn new-program
  [done]
  (-> (js/fetch (entity-url :programs) #js {:method "POST"
                                            :headers #js {"Content-Type" "application/json"}
                                            :body (js/JSON.stringify #js {:label "New Program"
                                                                          :data "{}"})})
      ;TODO check for non 201 status codes
      (.then #(.json %))
      (.then #(done (.-id %)))
      (.catch (fn [error]
                (js/console.log "error requesting a new program")))))


(defn patch-program 
  [program-id patch]
  (let [chan (async/chan)
        ; Yes I know it is double serialized. Get over it.
        body (-> (if (contains? patch :data) 
                   (update patch :data js/JSON.stringify)
                   patch)
                 clj->js
                 js/JSON.stringify)]
    (-> (js/fetch (entity-url :programs program-id) 
                  #js {:method "PATCH"
                       :headers #js {"Content-Type" "application/json"}
                       :body body})
        (.then (fn [resp]
                 (js/console.log "Done patching program" resp)
                 (async/go
                   (async/>! chan resp)
                   (async/close! chan))))
        (.catch (fn [error]
                  (js/console.log "Error patching program")
                  (async/go 
                    (async/>! chan error)
                    (async/close! chan)))))
    chan))


(defn deploy-program
  [program-id]
  (-> (js/fetch (entity-url :programs program-id :deploy)
                #js {:method "POST"})))


(defn delete-program
  [program-id]
  (let [chan (async/chan)]
    (-> (js/fetch (entity-url :programs program-id)
                  #js {:method "DELETE"})
        (.catch #(async/go (async/close! chan)))
        (.then #(async/go (async/close! chan))))
    chan))


(defn resp-json
  [resp]
  (async/go 
    (let [[_ d] (async/<! (utils/js-promise->chan (.json resp)))]
      (js->clj d))))


(defn new-sound
  [label data]
  (utils/js-promise->chan (js/fetch (entity-url :sounds) 
                                    #js {:method "POST"
                                         :headers #js {"Content-Type" "application/json"}
                                         :body (js/JSON.stringify #js {:label label :data data})})))


(defn delete-sound
  [sound-id]
  (utils/js-promise->chan (js/fetch (entity-url :sounds sound-id)
                                    #js {:method "DELETE"})))


(defn get-one-sound
  [sound-id]
  (utils/js-promise->chan (js/fetch (entity-url :sounds sound-id))))


(defn fetch-sounds
  []
  (utils/js-promise->chan (js/fetch (entity-url :sounds))))


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


(defn fetch-wrapper
  [label-to-url-mapping component]
  (let [r (r/atom (zipmap (keys label-to-url-mapping)
                          (repeat {:state :loading})))]
    (doseq [[label url] label-to-url-mapping]
      (-> (js/fetch url)
          (.catch (fn [error]
                    (swap! r assoc label {:state :error :data error})))
          (.then (fn [resp]
                   (if (= 200 (.-status resp))
                     (.json resp) ; TODO better errors here.
                     (js/Promise.resolve nil))))
          (.then (fn [data]
                   (if (some? data)
                     (swap! r assoc label {:state :done :data (js->clj data)})
                     (swap! r assoc label {:state :error}))))))
    (fn []
      [component @r])))


; wrap fetch in core async?


(defn any-loading?
  [state]
  (some #(= :loading (:state %)) (vals state)))


(defn get-errored-urls
  [state]
  (into #{}
        (comp (filter (fn [[k {:keys [state]}]]
                        (= state :error)))
              (map key))
        state))


(defn just-data
  [state]
  (reduce (fn [d [k {:keys [data]}]]
            (assoc d k data))
          {}
          state))


