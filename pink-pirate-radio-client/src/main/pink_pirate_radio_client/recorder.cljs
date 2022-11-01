(ns pink-pirate-radio-client.recorder 
  (:require [clojure.core.async :as async]
            [pink-pirate-radio-client.http :as http] 
            [pink-pirate-radio-client.utils :as utils]
            [reagent.core :as r]))


(def sounds-list-state (r/atom {:status :loading}))


(defn reload-sound-list
  []
  (async/go 
    (let [[status resp] (async/<! (http/fetch-sounds))]
      (if (= :success status)
        (swap! sounds-list-state 
               assoc 
               :status :success 
               :data (async/<! (http/resp-json resp)))
        (do (swap! sounds-list-state assoc :status :error)
            (js/console.log "Error fetching sounds" resp))))))


(defn on-click-delete-list
  [id e]
  (when (js/confirm "Are you sure you want to delete this sound clip?")
    (async/go
      (async/<! (http/delete-sound id))
      (reload-sound-list))))


(defn on-click-list-play
  [sound-id state e]
  (async/go
    (swap! state assoc :status :loading)
    (let [[status resp] (async/<! (http/get-one-sound sound-id))]
      (if (= :success status)
        (swap! state assoc :status :loaded :data (-> (async/<! (http/resp-json resp))
                                                     (get "data")))
        (swap! state assoc :status :error)))))


(defn audio-control
  [{:strs [id label]}]
  (let [state (r/atom {:status :unloaded})]
    (fn []
      (let [{:keys [status data]} @state]
        [:li (str label " - ")
         (cond (= status :unloaded) 
               [:button {:type "button" :on-click (partial on-click-list-play id state)} "Play"]
               (= status :loading)
               [:button {:type "button" :disabled true} "Loading..."] 
               (= status :loaded)
               [:audio {:src data :controls true}]
               (= status :error)
               [:span "Error loading sound clip"])
         [:button {:type "button" 
                   :on-click (partial on-click-delete-list id)}
          "Delete"]]))))


(defn sounds-list
  []
  (reload-sound-list)
  (fn []
    (let [{:keys [status data]} @sounds-list-state]
      [:div
       [:h3 "Recordings"]
       (case status
         :loading [:div "Loading sounds..."]
         :success (into [:ul]
                        (map (fn [sound]
                               [audio-control sound]))
                        (sort-by #(get % "updated_at") data)) 
         :error [:div "Error loading sounds!"])])))


(def init-record-state {:label "New"
                        :status :new
                        :error-msg ""
                        :stream nil 
                        :recorder nil 
                        :url nil 
                        :chunks []})


(def record-state (r/atom init-record-state))


(defn reset-record-state 
  [{:keys [stream]}]
  (assoc init-record-state :stream stream))


(def max-sample-len-ms 60000)

(defn set-label
  [new-label]
  #(assoc % :label new-label))


(defn set-status
  [new-status]
  #(assoc % :status new-status))


(defn set-error-status
  [error-msg]
  #(assoc % :status :error :error-msg error-msg))


(defn get-stream
  []
  (if-let [stream (:stream @record-state)]
    (async/go stream)
    (let [c (async/chan)]
      (-> (js/navigator.mediaDevices.getUserMedia #js {:audio true})
          (.then (fn [stream]
                   (async/go
                     (swap! record-state assoc :stream stream)
                     (async/>! c stream)
                     (async/close! c))))
          (.catch (fn [err]
                    (async/go
                      (js/console.log "Error getting user media" err)
                      (async/>! c nil)
                      (async/close! c)))))
      c)))


(defn chunks->blob
  [[c :as chunks]]
  (js/Blob. (clj->js chunks) #js {:type (.-type c)}))


(defn chunks->base64
  [chunks]
  (let [ch (async/chan)
        reader (js/FileReader.)]
    (set! (.-onloadend reader)
          (fn []
            (async/go
              (async/>! ch (.-result reader))
              (async/close! ch))))
    (.readAsDataURL reader (chunks->blob chunks))
    ch))


(defn on-recorder-stop
  [e]
  (swap! record-state 
         (comp (set-status :done-recording)
               #(assoc % :url (js/window.URL.createObjectURL (chunks->blob (:chunks %)))))))


(defn on-label-change
  [e]
  (swap! record-state (set-label (.-value (.-target e)))))


(defn add-chunk
  [data]
  (swap! record-state update :chunks conj data))


(defn on-click-stop
  [e]
  (let [{:keys [recorder stop-timeout-id]} @record-state]
    (.stop recorder)
    (js/clearTimeout stop-timeout-id)))


(defn on-click-record
  [e]
  (swap! record-state (set-status :initializing))
  (async/go
    (if-let [stream (async/<! (get-stream))]
      (let [recorder (js/MediaRecorder. stream)]
        (set! (.-onstop recorder) on-recorder-stop)
        (set! (.-ondataavailable recorder) #(add-chunk (.-data %)))
        (.start recorder)
        (swap! record-state 
               assoc 
               :status :recording 
               :recorder recorder
               :stop-timeout-id (js/setTimeout on-click-stop max-sample-len-ms)))
      (swap! record-state assoc :status :error :error-msg "Error turning on microphone!"))))


(defn on-click-delete
  [e]
  (swap! record-state reset-record-state))



(defn on-click-save
  [e]
  (swap! record-state (set-status :saving))
  ; TODO actual send data to the server!
  (async/go 
    (let [{:keys [chunks label]} @record-state
          data (async/<! (chunks->base64 (:chunks @record-state)))
          [status resp] (async/<! (http/new-sound label data))]
      (if (= :success status)
        (do (reload-sound-list)
            (swap! record-state reset-record-state))
        (swap! record-state (set-error-status "Error saving sounds!"))))))


(defn recorder 
  []
  (let [{:keys [label status error-msg url] :as rs} @record-state]
    [:div 
     [:h3 "New Recording"]
     [:input {:type "text"
              :disabled (not (contains? #{:new :done-recording} status))
              :value label
              :on-change on-label-change}]
     [:div
      (case status
        :new [:button {:type "button" :on-click on-click-record} "Record"]
        :initializing [:span "Initializing"]
        :recording [:div 
                    [:span "Recording..."]
                    [:button {:type "button" :on-click on-click-stop} "Stop"]]
        :done-recording [:div 
                         [:audio {:src url :controls true}]
                         [:div
                          [:button {:type "button" :on-click on-click-save} "Save"]
                          [:button {:type "button" :on-click on-click-delete} "Delete"]]] 
        :saving [:span "Saving..."]
        :error [:span "Error:" error-msg])]]))


(defn editor
  []
  [:div
   [:h1 "Sounds Clips"]
   [:button {:type "button" :on-click utils/go-home!} "Back"]
   [recorder]
   [sounds-list]])
