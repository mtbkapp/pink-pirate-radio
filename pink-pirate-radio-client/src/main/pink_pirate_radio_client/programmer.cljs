(ns pink-pirate-radio-client.programmer
  (:require [reagent.core :as r]
            ["blockly" :as Blockly])) 

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
                                                                :options #js [#js ["bark" "BARK"]
                                                                              #js ["growl" "GROWL"]
                                                                              #js ["whine" "WHINE"]]}]
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
                                                                :options #js [#js ["Back Pocket" "Back Pocket"]
                                                                              #js ["Lee" "Lee"]
                                                                              #js ["Ballet" "Ballet"]]}]
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


(defn inject-blockly
  [container]
  (Blockly/inject container 
                  #js {:toolbox (js/document.getElementById "toolbox-categories")
                      ; :zoom #js {:controls true
                      ;            :wheel true
                      ;            :pinch true}
                      ; 
                       }))


(defn on-workspace-change
  [workspace]
  ; debounce
  ; serialize
  ; send to server
  (js/console.log (Blockly/serialization.workspaces.save workspace))
  )


(def workspace (atom nil))


(defn ui
  []
  (r/create-class 
    {:component-did-mount 
     (fn [this]
       ;TODO load program here from server
       ;TODO load songs, sounds clips, and drawings
       ;once that is all done then inject Blockly 
       ; do it like this because the blocks need to have the data embedded into them
       (let [refs (.-refs this)
             new-workspace (inject-blockly (.-container ^object refs))]
         (.addChangeListener ^object new-workspace 
                             (fn [_] (on-workspace-change new-workspace)))
         (reset! workspace new-workspace)))
     :render
     (fn [this]
       [:div
        [:div {:id "programmer-toolbar"}
         [:button {:type "button"} "Close"]
         [:button {:type "button" :on-click #(.undo ^object @workspace false)} "Undo"]
         [:button {:type "button" :on-click #(.undo ^object @workspace true)} "Redo"]
         [:button {:type "button"} "Deploy!"]]
        [:div {:ref "container" :id "blockly" :style {:width 800 :height 600}}]])}))
