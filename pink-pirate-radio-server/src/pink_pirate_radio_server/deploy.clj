(ns pink-pirate-radio-server.deploy
  (:require [clojure.java.io :as io] 
            [pink-pirate-radio-server.compiler :as compiler])
  (:import [java.lang ProcessBuilder$Redirect]
           [java.time LocalDateTime]
           [java.util List]))


(set! *warn-on-reflection* true)


(def py-process (atom nil))

(def lock (Object.))

(defn stop-program
  []
  (when-let [^Process p @py-process]
    (.destroy p)
    (reset! py-process nil)))


(defn deploy-py
  [py-code]
  (locking lock 
    (stop-program)
    (let [t (LocalDateTime/now)
          program-file (io/file (str "py_run_program_" t ".py"))
          _ (spit program-file py-code)
          out-file (io/file (str "py_run_output_" t ".log"))
          ^List cmd ["python3" "-u" (.getCanonicalPath program-file)]
          process (-> (ProcessBuilder. cmd)
                      (.redirectErrorStream true)
                      (.redirectOutput out-file)
                      (.start))]
      (swap! py-process (constantly process)))))


(defn deploy-program
  [program]
  ; TODO logging
  (-> program
      compiler/compile-program
      compiler/emit-py
      compiler/prepend-preamble
      compiler/append-kick
      deploy-py)
  {})
