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
  [py-code dir]
  (locking lock 
    (stop-program)
    (let [program-file (io/file dir "program.py")
          _ (spit program-file py-code)
          out-file (io/file dir "out.log" )
          ^List cmd ["python3" "-u" (.getCanonicalPath program-file)]
          process (-> (ProcessBuilder. cmd)
                      (.directory dir)
                      (.redirectErrorStream true)
                      (.redirectOutput out-file)
                      (.start))]
      (swap! py-process (constantly process)))))


(defn write-sound-clips
  [dir db]
  (let [sound-dir (io/file dir "sound_clips")]
    (.mkdirs sound-dir)
    (doseq [{:keys [id data]} (jdbc/execute! db 
                                             ["select id, data from entity where kind = 'sounds'"]
                                             {:builder-fn rs/as-unqualified-lower-maps})]
      (io/copy (.decode (Base64/getDecoder) 
                        ^String (second (string/split data #",")))
               (io/file sound-dir (str id))))))


(defn deploy-program
  [db program]
  ; TODO logging
  ; TODO clean up old folder(s) before running a new program
  (let [t (LocalDateTime/now)
        dir (doto (io/file "py_work_dirs" (str "py_" t))
              (.mkdirs))] 


    (write-sound-clips dir db)
    (-> program
        compiler/compile-program
        compiler/emit-py
        compiler/prepend-preamble
        compiler/append-kick
        (deploy-py dir))
    {:dir (.getCanonicalPath dir)}))
