(ns pink-pirate-radio-server.compiler
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as string]))


(defn get-field
  [block k]
  (get-in block ["fields" k]))


(defn get-input-field
  [block k]
  (get-in block ["inputs" k]))


(defn get-block-or-shadow
  [{:strs [block shadow]}]
  (if (some? block) block shadow))


(defn get-input-expr
  [block k]
  (get-block-or-shadow (get-input-field block k)))


(defn to-lower-keyword
  [s]
  (keyword (string/lower-case s)))


(defn get-op
  [block]
  (to-lower-keyword (get-field block "OP")))


(declare compile-expr)
(declare compile-stmt)
(declare get-handler-stmts)


(defn compile-input-expr
  [block k]
  (compile-expr (get-input-expr block k)))


(defn compile-sub-block
  [block k]
  (map compile-stmt (get-handler-stmts (get-input-field block k) [])))


(def compile-expr nil)
(defmulti compile-expr (fn [{t "type"}] t))


(defmethod compile-expr :default
  [block]
  (clojure.pprint/pprint ["UNKNOWN EXPR:" block])
  :unknown-expr)


(defmethod compile-expr nil
  [block]
  nil)


(defmethod compile-expr "math_number"
  [block]
  (get-field block "NUM"))


(defn math-single
  [block]
  [(get-op block) (compile-input-expr block "NUM")])


(defmethod compile-expr "math_single"
  [block]
  (math-single block))


(defmethod compile-expr "math_trig"
  [block]
  (math-single block))


(defmethod compile-expr "math_constant"
  [block]
  [:get_constant (string/lower-case (get-field block "CONSTANT"))])


(defmethod compile-expr "math_random_int"
  [block]
  [:rand_int 
   (compile-input-expr block "FROM")
   (compile-input-expr block "TO")])


(defmethod compile-expr "math_number_property"
  [block]
  (let [prop (get-field block "PROPERTY")]
    (cond->
      [(to-lower-keyword prop)
       (compile-input-expr block "NUMBER_TO_CHECK")]
      (= prop "DIVISIBLE_BY") (conj (compile-input-expr block "DIVISOR")))))


(defmethod compile-expr "math_round"
  [block]
  [(get-op block) (compile-input-expr block "NUM")])


(defmethod compile-expr "math_random_float"
  [block]
  [:rand_float])


(defmethod compile-expr "variables_get"
  [block]
  [:get_var (get (get-field block "VAR") "id")])


(defmethod compile-expr "logic_ternary"
  [block]
  [:ternary 
   (compile-input-expr block "IF")
   (compile-input-expr block "THEN")
   (compile-input-expr block "ELSE")])


(defn binary-op
  [block]
  [(get-op block) 
   (compile-input-expr block "A") 
   (compile-input-expr block "B")])


(defmethod compile-expr "math_arithmetic"
  [block]
  (binary-op block))


(defmethod compile-expr "logic_compare"
  [block]
  (binary-op block))


(defmethod compile-expr "logic_operation"
  [block]
  [(keyword (str "logic_" (name (get-op block)))) 
   (compile-input-expr block "A") 
   (compile-input-expr block "B")])


(defmethod compile-expr "math_modulo"
  [block]
  [:mod 
   (compile-input-expr block "DIVIDEND") 
   (compile-input-expr block "DIVISOR")])


(defmethod compile-expr "math_constrain"
  [block]
  [:constrain
   (compile-input-expr block "VALUE")
   (compile-input-expr block "LOW")
   (compile-input-expr block "HIGH")])


(defmethod compile-expr "logic_boolean"
  [block]
  (= "TRUE" (get-field block "BOOL")))


(defmethod compile-expr "logic_negate"
  [block]
  [:logic_negate (compile-input-expr block "BOOL")])


(defmethod compile-expr "colour_rgb"
  [block]
  [:color 
   (compile-input-expr block "RED")
   (compile-input-expr block "GREEN")
   (compile-input-expr block "BLUE")])


(defmethod compile-expr "colour_random"
  [block]
  [:rand_color])


(defmethod compile-expr "colour_picker"
  [block]
  (let [hex-code (get-field block "COLOUR")
        f #(Long/parseLong (subs hex-code % (+ % 2)) 16)]
    [:color (f 1) (f 3) (f 5)]))


(defmethod compile-expr "colour_blend"
  [block]
  [:color_blend 
   (compile-input-expr block "COLOUR1")
   (compile-input-expr block "COLOUR2")
   (compile-input-expr block "RATIO")])


(def compile-stmt nil)
(defmulti compile-stmt (fn [{t "type"}] t))


(defmethod compile-stmt "set_display_color"
  [block]
  [:set_display_color (compile-input-expr block "VALUE")])


(defmethod compile-stmt "display_emoji"
  [block]
  [:display_emoji (get-field block "VALUE")])


(defmethod compile-stmt "variables_set"
  [block]
  [:set_var 
   (get (get-field block "VAR") "id")
   (compile-input-expr block "VALUE")])


(defmethod compile-stmt "play_sound_clip"
  [block]
  [:play_sound_clip (get-field block "clip")])


(defmethod compile-stmt "controls_repeat_ext"
  [block]
  [:repeat 
   (compile-input-expr block "TIMES") 
   (compile-sub-block block "DO") ])


(defmethod compile-stmt "controls_if"
  [block]
  (let [{el-if-count "elseIfCount" else? "hasElse"} (get block "extraState")]
    (cond->
      (into [:cond]
            (map (fn [i]
                   [(compile-input-expr block (str "IF" i))
                    (compile-sub-block block (str "DO" i))]))
            (range 0 (inc (or el-if-count 0))))
      else? (conj [:else (compile-sub-block block "ELSE")]))))


(defmethod compile-stmt "math_change"
  [block]
  (let [var-id (get (get-field block "VAR") "id")]
    [:set_var
     var-id
     [:add 
      [:get_var var-id]
      (compile-input-expr block "DELTA")]]))


(defmethod compile-stmt "media_action"
  [block]
  [:media_player_action (get-field block "button")])


(defmethod compile-stmt "wait"
  [block]
  [:wait (compile-input-expr block "VALUE")])


(defmethod compile-stmt "controls_whileUntil"
  [block]
  [(to-lower-keyword (get-field block "MODE"))
   (compile-input-expr block "BOOL")
   (compile-sub-block block "DO")])


(defmethod compile-stmt "controls_for"
  [block]
  [:for 
   (get (get-field block "VAR" ) "id")
   (compile-input-expr block "FROM")
   (compile-input-expr block "TO")
   (compile-input-expr block "BY")
   (compile-sub-block block "DO")])


(defmethod compile-stmt :default
  [block]
  (clojure.pprint/pprint ["UNKNOWN STMT:" block])
  :unknown-stmt)


(defn get-button
  [btn-handler-block]
  (-> (get-in btn-handler-block ["fields" "button"])
      (string/replace #"^btn_" "")
      (keyword)))


(defn get-handler-stmts 
  ([{:strs [block]} stmts]
   (if (some? block)
     (recur (get block "next") (conj stmts (dissoc block "next")))
     stmts))
  ([handler-block]
   (get-handler-stmts (get-in handler-block ["inputs" "handler"]) [])))


(defn compile-handler-block
  [handler-block]
  (map compile-stmt (get-handler-stmts handler-block)))


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


(defn compile-program 
  [{:strs [variables blocks] :as blockly-program}]
  (compile-event-handlers {} blocks))


(set! *warn-on-reflection* true)


(def noop "1==1")
(def tab "    ")


(def emit-expr nil)
(defmulti emit-expr (fn [sb expr]
                      (cond
                        (vector? expr) :call
                        (boolean? expr) :bool
                        (number? expr) :num
                        (string? expr) :str
                        (nil? expr) :nil)))


(defmethod emit-expr :call
  [^StringBuilder sb [f & args]]
  (.append sb (name f))
  (.append sb "(")
  (doseq [a args]
    (emit-expr sb a)
    (.append sb ","))
  (when-not (empty? args)
    (.deleteCharAt sb (dec (.length sb))))
  (.append sb ")"))


(defmethod emit-expr :bool
  [^StringBuilder sb expr]
  (.append sb (if expr "True" "False")))


(defmethod emit-expr :nil
  [^StringBuilder sb expr]
  (.append sb "None"))


(defmethod emit-expr :num
  [^StringBuilder sb expr]
  (.append sb expr))


(defmethod emit-expr :str
  [^StringBuilder sb expr]
  (.append sb "\"")
  (.append sb expr)
  (.append sb "\""))


(defmethod emit-expr :default
  [sb expr]
  (throw (ex-info "Unknown Expression" {:py (str sb) :expr expr})))


(defn emit-tabs
  [tab-count ^StringBuilder sb]
  (dotimes [_ tab-count]
    (.append sb tab)))


(declare emit-block)

(def emit-stmt nil)
(defmulti emit-stmt (fn [tab-count sb [f & args]] f))

(defmethod emit-stmt :default
  [tab-count ^StringBuilder sb stmt]
  (emit-tabs tab-count sb)
  (emit-expr sb stmt)
  (.append sb \newline))


(defmethod emit-stmt :repeat
  [tab-count ^StringBuilder sb [_ times-expr block]]
  (emit-tabs tab-count sb)
  (.append sb "for i in range(")
  (emit-expr sb times-expr)
  (.append sb "):")
  (.append sb \newline)
  (emit-block (inc tab-count) sb block))


(defmethod emit-stmt :cond
  [tab-count ^StringBuilder sb [_ [if-cond if-do] & pairs]]
  (emit-tabs tab-count sb)
  (.append sb "if (")
  (emit-expr sb if-cond)
  (.append sb "):\n")
  (emit-block (inc tab-count) sb if-do)
  (doseq [[cond-expr block] pairs]
    (if (= :else cond-expr)
      (do 
        (emit-tabs tab-count sb)
        (.append sb "else:\n"))
      (do
        (emit-tabs tab-count sb)
        (.append sb "elif (")
        (emit-expr sb cond-expr)
        (.append sb "):\n")))
    (emit-block (inc tab-count) sb block)))


(defmethod emit-stmt :for
  [tab-count ^StringBuilder sb [_ v from to by block]]
  (emit-stmt tab-count sb [:set_var v from])
  (emit-tabs tab-count sb)
  (.append sb "while (")
  (emit-expr sb [:lte [:get_var v] to])
  (.append sb "):\n")
  (emit-block (inc tab-count) sb block)
  (emit-stmt (inc tab-count) sb [:set_var v [:add [:get_var v] by]]))


(defmethod emit-stmt :while
  [tab-count ^StringBuilder sb [_ cond-expr block]]
  (emit-tabs tab-count sb)
  (.append sb "while (")
  (emit-expr sb cond-expr)
  (.append sb "):\n")
  (emit-block (inc tab-count) sb block))


(defmethod emit-stmt :until
  [tab-count ^StringBuilder sb [_ cond-expr block]]
  (emit-tabs tab-count sb)
  (.append sb "while True:\n")
  (emit-block (inc tab-count) sb block)
  (emit-tabs (inc tab-count) sb)
  (.append sb "if ")
  (emit-expr sb cond-expr)
  (.append sb ":\n")
  (emit-tabs (+ tab-count 2) sb)
  (.append sb "break\n"))


(defn emit-handler-fn-dec
  [^StringBuilder sb handler-name]
  (.append sb "def " )
  (when (keyword? handler-name)
    (.append sb (name handler-name)))
  (when (vector? handler-name)
    (.append sb (string/join "_" (map name handler-name))))
  (.append sb "():")
  (.append sb \newline))


(defn emit-block
  [tab-count ^StringBuilder sb block]
  (doseq [stmt block]
    (emit-stmt tab-count sb stmt)))


(defn emit-handler
  [^StringBuilder sb [handler-name blocks]]
  (emit-handler-fn-dec sb handler-name)
  (if (every? empty? blocks)
    (do (emit-tabs 1 sb) 
        (.append sb noop)
        (.append sb \newline))
    (doseq [b blocks]
      (emit-block 1 sb b)))
  (.append sb \newline))


(defn emit-py 
  ([ir]
   (let [sb (StringBuilder.)]
     (emit-py sb ir)
     (str sb)))
  ([sb {:keys [handlers]}]
   (doseq [h handlers]
     (emit-handler sb h))))


(defn prepend-preamble
  [py]
  (str (slurp (io/resource "preamble.py")) py))


(defn append-kick
  [py]
  (str py (slurp (io/resource "kick.py"))))

