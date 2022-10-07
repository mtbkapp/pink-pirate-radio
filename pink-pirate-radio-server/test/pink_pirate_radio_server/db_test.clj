(ns pink-pirate-radio-server.db-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [pink-pirate-radio-server.db :refer :all]
            [pink-pirate-radio-server.core :as core])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))


(defmacro with-test-db
  [[db-name] & body]
  `(let [file# (str (Files/createTempFile "pink_pirate_radio" "sqlite" (make-array FileAttribute 0)))
         ~db-name (component/start (core/->SqliteDb (str "jdbc:sqlite:" file#)))]
     (try
       ~@body
       (finally 
         (component/stop ~db-name) ))))


(deftest test-crud 
  (with-test-db [db]
    (is (= [] (list-of db :sounds)))
    (is (nil? (get-one db :sounds 1)))
    (is (nil? (patch db :sounds 1 {:label "donuts"})))
    (is (false? (delete db :sounds 1)))
    (let [create0 (create db :sounds {:label "donuts" :data "mmmmmm"})]
      (is (= #{:id :updated_at :created_at :kind :label :data}
             (set (keys create0))))
      (is (= 1 (:id create0)))
      (is (int? (:updated_at create0)))
      (is (int? (:created_at create0)))
      (is (= "sounds" (:kind create0)))
      (is (= "donuts" (:label create0)))
      (is (= "mmmmmm" (:data create0)))
      (is (= create0 (get-one db :sounds (:id create0))))
      (is (= [(dissoc create0 :kind :data)]
             (list-of db :sounds)))
      (let [patch0 (patch db :sounds 1 {:label "doughnuts"})]
        (is (<= (:created_at patch0) (:updated_at patch0)))
        (is (= (:created_at patch0) (:created_at create0)))
        (is (= "doughnuts" (:label patch0)))
        (is (= "mmmmmm" (:data patch0)))
        (is (= patch0 (get-one db :sounds (:id create0))))
        (is (= [(dissoc patch0 :kind :data)]
               (list-of db :sounds))))
      (is (true? (delete db :sounds 1)))
      (is (false? (delete db :sounds 1)))
      (is (nil? (get-one db :sounds 1)))
      (is (= [] (list-of db :sounds))))))

