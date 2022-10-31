(ns pink-pirate-radio-server.deploy
  (:require [clojure.java.io :as io] 
            [clojure.string :as string]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [pink-pirate-radio-server.compiler :as compiler])
  (:import [java.lang ProcessBuilder$Redirect]
           [java.time LocalDateTime]
           [java.util Base64 List]))


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


(defn write-sound-clips
  [db]
  (doseq [{:keys [id data]} (jdbc/execute! db 
                                           ["select id, data from entity where kind = 'sounds'"]
                                           {:builder-fn rs/as-unqualified-lower-maps})]
    (io/copy (.decode (Base64/getDecoder) 
                      ^String (second (string/split data #",")))
             (io/file "clips" (str id)))))


(defn deploy-program
  [db program]
  ; TODO logging
  ; TODO create a folder for everything to run in
  ; TODO clean up folder before running a new program
  (write-sound-clips db)
  #_(-> program
      compiler/compile-program
      compiler/emit-py
      compiler/prepend-preamble
      compiler/append-kick
      deploy-py)
  {})
