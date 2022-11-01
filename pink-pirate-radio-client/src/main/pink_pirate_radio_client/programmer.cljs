(ns pink-pirate-radio-client.programmer
  (:require [clojure.core.async :as async]
            [clojure.string :as string]
            [reagent.core :as r]
            ["blockly" :as Blockly]
            [pink-pirate-radio-client.http :as http]
            [pink-pirate-radio-client.utils :as utils])) 

; custom blocks
; play sound, async?
; scroll text, async?
; on button push
; set photo
; play gif?
; set background color - also with any image?
; sprites? images that go over the background?


; hardware
; - screen
;   - gif
;   - saved drawing
;   - one color
; - buttons
;   - labeled A,B,X,Y
; - music player
;   - plays async (via vlc lib)
;   - songs come from a folder on server
;   - need a song block
;   - can get songs from api
;   - need a play a list of songs block by reference or by variable
; - clip player
;   - plays a saved audio clip
;   - user can upload new recorded clips (use browser apis to record)

; TODO set colors


(def workspace (atom nil))
(def options (atom {:sounds [] :drawings [] :songs []}))


(defn get-options-as-js
  [kind]
  (clj->js (as-> (get @options kind) opts
             (map (fn [{:strs [id label]}]
                    [label (str id)]) 
                  opts)
             (conj opts ["None" "none"]))))


(set! Blockly/Blocks.set_display_color
      #js {:init (fn []
                   (this-as
                     t
                     (.jsonInit ^object t #js {:message0 "Set the display color to %1"
                                               :type "set_display_color"
                                               :args0 #js [#js {:type "input_value"
                                                                :name "VALUE"
                                                                :check "Colour"}]
                                               :previousStatement nil
                                               :nextStatement nil
                                               :colour 240
                                               :tooltip "Set the color of the Pink Pirate Radio's display"})))})


(set! Blockly/Blocks.on_button_push
      #js {:init (fn []
                   (this-as
                     t
                     (.jsonInit ^object t #js {:message0 "on button press %1 %2"
                                               :type "on_button_push"
                                               :args0 #js [#js {:type "field_dropdown"
                                                                :name "button"
                                                                :options #js [#js ["A" "btn_a"]
                                                                              #js ["B" "btn_b"]
                                                                              #js ["X" "btn_x"]
                                                                              #js ["Y" "btn_y"]]}
                                                           #js {:type "input_statement"
                                                                :name "handler"}]
                                               :colour 240
                                               :tooltip "on button press"})))})



(set! Blockly/Blocks.on_program_start
      #js {:init (fn []
                   (this-as
                     t
                     (.jsonInit ^object t #js {:message0 "on program start %1"
                                               :type "on_program_start"
                                               :args0 #js [#js {:type "input_statement"
                                                                :name "handler"}]
                                               :colour 240
                                               :tooltip "on program start"})))})



(set! Blockly/Blocks.media_action
      #js {:init (fn []
                   (this-as
                     t
                     (.jsonInit ^object t #js {:message0 "%1 the media player"
                                               :type "media_action"
                                               :args0 #js [#js {:type "field_dropdown"
                                                                :name "button"
                                                                :options #js [#js ["pause" "media_pause"]
                                                                              #js ["play" "media_play"]
                                                                              #js ["next" "media_next"]
                                                                              #js ["previous" "media_previous"]]}]
                                               :previousStatement nil
                                               :nextStatement nil
                                               :colour 240
                                               :tooltip "Perform the selected action on the media player"})))})


(set! Blockly/Blocks.media_set_playlist
      #js {:init (fn []
                   (this-as
                     t
                     (.jsonInit ^object t #js {:message0 "Set the media players playlist to %1"
                                               :type "media_set_playlist"
                                               :args0 #js [#js {:type "input_value"
                                                                :name "VALUE"
                                                                :check #js ["Array" "Variable"]}]
                                               :previousStatement nil
                                               :nextStatement nil
                                               :colour 240
                                               :tooltip "Set the media players playlist"})))})


(set! Blockly/Blocks.play_sound_clip
      #js {:init (fn []
                   (this-as
                     t
                     (.jsonInit ^object t #js {:message0 "play sound clip %1"
                                               :type "play_sound_clip"
                                               :args0 #js [#js {:type "field_dropdown"
                                                                :name "clip"
                                                                :options (partial get-options-as-js :sounds)}]
                                               :previousStatement nil
                                               :nextStatement nil
                                               :colour 240
                                               :tooltip "Play the selected sound clip"})))})


(set! Blockly/Blocks.song
      #js {:init (fn []
                   (this-as
                     t
                     (.jsonInit ^object t #js {:message0 "song: %1"
                                               :args0 #js [#js {:type "field_dropdown"
                                                                :name "clip"
                                                                :options (partial get-options-as-js :songs)}]
                                               :output "String"
                                               :colour 160
                                               :tooltip "Returns the number of letters in the provided text."})))})


(set! Blockly/Blocks.wait
      #js {:init (fn []
                   (this-as
                     t
                     (.jsonInit ^object t #js {:message0 "wait for %1 seconds"
                                               :type "wait"
                                               :args0 #js [#js {:type "input_value"
                                                                :name "VALUE"
                                                                :check "Number"}]
                                               :previousStatement nil
                                               :nextStatement nil
                                               :colour 240
                                               :tooltip "wait"})))})


(set! Blockly/Blocks.display_emoji
      #js {:init (fn []
                   (this-as
                     t
                     (.jsonInit ^object t #js {:message0 "Display Emoji %1"
                                               :type "display_emoji"
                                               :args0 #js [#js {:type "field_input"
                                                                :name "VALUE"
                                                                :check "String"}]
                                               :previousStatement nil
                                               :nextStatement nil
                                               :colour 240
                                               :tooltip "display_emoji"})))})


(defn inject-blockly
  [container]
  (Blockly/inject container 
                  #js {:toolbox (js/document.getElementById "toolbox-categories")
                      ; :zoom #js {:controls true
                      ;            :wheel true
                      ;            :pinch true}
                      ; 
                       }))


(def patch-program-chan
  (let [chan (async/chan (async/sliding-buffer 2))]
    (async/go
      (while true
        (let [[program-id patch] (async/<! chan)]
          (async/<! (http/patch-program program-id patch)))))
    chan))


(defn confirm-delete 
  [program-id e]
  (when (js/confirm "Are you sure you want to throw this program away? It will be gone forever.")
    (async/go 
      (async/<! (http/delete-program program-id))
      (utils/go-home!))))


(defn program-label-editor
  [program-id label]
  (let [curr-label (r/atom label)]
    (fn []
      [:div 
       [:label {:for "program-label-input"}]
       [:input {:id "program-label-input" 
                :type "text"
                :value @curr-label
                :on-change (fn [e]
                             (let [new-label (.-value (.-target e))]
                               (reset! curr-label new-label)
                               (async/go
                                 (async/>! patch-program-chan 
                                           [program-id {:label new-label}]))))}]])))


(defn build-options
  [api-sounds]
  (map (fn [{:strs [id label]}] [id label]) api-sounds))


(defn blockly-editor 
  [{:keys [program sounds drawings]}]
  (prn sounds)
  (r/create-class 
    {:component-did-mount 
     (fn [this]
       (swap! options assoc :sounds sounds :drawings drawings)
       (let [refs (.-refs this)
             new-workspace (inject-blockly (.-container ^object refs))]
         (Blockly/serialization.workspaces.load (js/JSON.parse (get program "data"))
                                                new-workspace)
         (.addChangeListener ^object new-workspace 
                             (fn []
                               (async/go
                                 (async/>! patch-program-chan 
                                           [(get program "id")
                                            {:data (Blockly/serialization.workspaces.save new-workspace)}]))))
         (reset! workspace new-workspace)))
     :render
     (fn [this]
       (let [{:keys [program]} (r/props this)
             {:strs [id label]} program]
         [:div
          [program-label-editor id label]
          [:div {:id "programmer-toolbar"}
           [:button {:type "button" :on-click #(utils/go-home!)} "Done"]
           [:button {:type "button" :on-click (partial confirm-delete id)} "Delete"]
           [:button {:type "button" :on-click #(.undo ^object @workspace false)} "Undo"]
           [:button {:type "button" :on-click #(.undo ^object @workspace true)} "Redo"]
           [:button {:type "button" :on-click #(http/deploy-program id)} "Run"] ; TODO validate program in some way first? 
           [:button {:type "button" :on-click #(http/stop-program)} "Stop!"]] 
          [:div {:ref "container" :id "blockly" :style {:width 800 :height 600}}]]))}))


(defn editor*
  [state]
  (let [error-ks (http/get-errored-urls state)]
    (cond (http/any-loading? state)
          [:div "Loading..."]
          (not (empty? error-ks))
          [:div "Error loading: " (string/join "," (map name error-ks))]
          :else 
          [blockly-editor (http/just-data state)])))


(defn editor
  [program-id]
  (http/fetch-wrapper
    {:program (http/entity-url :programs program-id)
     :sounds (http/entity-url :sounds)
     :drawings (http/entity-url :drawings)
     :songs (http/entity-url :songs)}
    editor*))
