(ns pink-pirate-radio-server.compiler-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]))


(def test-program1 (edn/read-string (slurp (io/resource "test_program1.edn"))))


(keys test-program1)


(clojure.pprint/pprint (get test-program1 "variables"))


(defn init-variables
  [compiler variables]
  (reduce (fn [compiler {:strs [name id]}]
            (update compiler :variables assoc id name))
          compiler
          variables))


(defn compile-statements
  [x]
  x)


(defn get-button
  [btn-handler-block]
  (-> (get-in btn-handler-block ["fields" "button"])
      (string/replace #"^btn_" "")
      (keyword)))


(defn get-handler
  [handler-block]
  (get-in handler-block ["inputs" "handler"]))


(defn compile-handler-block
  [handler-block]
  (let [handler-stmts (get-handler handler-block)]
    (compile-statements handler-stmts)))


(defn compile-event-handlers
  [compiler {:strs [blocks]}]
  (reduce (fn [compiler {:strs [type] :as top-level-block}]
            (cond (= "on_program_start" type)
                  (update-in compiler [:handlers :on_program_start] conj (compile-handler-block top-level-block))
                  (= "on_button_push" type)
                  (update-in compiler [:handlers [:on_button_push (get-button top-level-block)]] conj (compile-handler-block top-level-block))
                  :else 
                  compiler))
          compiler
          blocks))

(defn init 
  [{:strs [variables blocks] :as blockly-program}]
  (-> {}
      (init-variables variables)
      (compile-event-handlers blocks)))



(clojure.pprint/pprint (init test-program1))


; the top level blocks should only be the event handlers, which are
; 1. on program start
; 2. on button press
;
; there may be multiple handlers so they should be grouped and put into any
; order.
