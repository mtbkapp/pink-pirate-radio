(ns pink-pirate-radio-server.handlers-test
  (:require [clj-http.client :as http]
            [clojure.string :as string]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [pink-pirate-radio-server.core :as core]
            [pink-pirate-radio-server.deploy :as deploy])
  (:import [java.net URL]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))


(defn with-test-system
  [tests]
  (let [file (str (Files/createTempFile "pink_pirate_radio" "sqlite" (make-array FileAttribute 0)))
        system (component/start (core/system "localhost" 9876 file))]
    (try
      (tests)
      (finally
        (component/stop system)))))


(use-fixtures :each with-test-system)


(defn url
  [segments]
  (str (URL. "http" "localhost" 9876 (str "/" (string/join "/" segments)))))

(defn entity-url 
  ([kind] (url ["entities" (name kind)]))
  ([kind id] (url ["entities" (name kind) id])))


(defn http-get-all
  [kind]
  (http/request {:method :get
                 :url (entity-url kind)
                 :throw-exceptions false
                 :as :json}))


(defn http-get-one
  [kind id]
  (http/request {:method :get
                 :url (entity-url kind id)
                 :throw-exceptions false
                 :as :json}))


(defn http-post
  [kind data]
  (http/request {:method :post
                 :url (entity-url kind) 
                 :form-params data
                 :content-type :json
                 :throw-exceptions false
                 :as :json}))


(defn http-patch
  [kind id data]
  (http/request {:method :patch
                 :url (entity-url kind id)
                 :form-params data
                 :content-type :json
                 :throw-exceptions false
                 :as :json}))


(defn http-delete 
  [kind id]
  (http/request {:method :delete
                 :url (entity-url kind id)
                 :throw-exceptions false
                 :as :json}))


(deftest test-crud-http 
  (testing "not found and empty"
    (is (= 404 (:status (http-get-one :sounds 1))))
    (is (= 404 (:status (http-patch :sounds 1 {:label "pirate"}))))
    (is (= 404 (:status (http-delete :sounds 1))))
    (let [list-of-resp (http-get-all :sounds)]
      (is (= 200 (:status list-of-resp)))
      (is (= [] (:body list-of-resp)))))
  (testing "writes"
    (let [create0 (http-post :sounds {:label "donuts" :data "mmm"})
          create1 (http-post :sounds {:label "doughnuts" :data "mmm-mmm"})
          create2 (http-post :drawings {:label "poop emoji" :data "nah"})
          patch (http-patch :sounds 2 {:data "mmm mmm mmm"})
          read-one0 (http-get-one :sounds 1)
          read-one1 (http-get-one :sounds 2)
          read-one2 (http-get-one :drawings 3)
          read-one3 (http-get-one :sounds 3)
          read-all-sounds (http-get-all :sounds)
          read-all-drawing (http-get-all :drawings)]
      (is (= 201 (:status create0) (:status create1) (:status create2)))
      (is (= 200 (:status read-one0) (:status read-one1) (:status read-one2)))
      (is (= 404 (:status read-one3)))
      (is (= 200 (:status read-all-sounds) (:status read-all-drawing)))
      (is (= (into #{} 
                   (map #(select-keys % [:id :updated_at :created_at :label]))
                   [(:body create0) (:body create1)])
             (into #{} 
                   (map #(select-keys % [:id :updated_at :created_at :label]))
                   [(:body read-one0) (:body read-one1)])
             (set (:body read-all-sounds))))
      (is (= 1 (:id (:body create0))))
      (is (int? (:updated_at (:body create0))))
      (is (int? (:created_at (:body create0))))
      (is (= "donuts" (:label (:body create0))))
      (is (= "mmm" (:data (:body create0)))))
    (testing "delete"
      (is (= 2 (count (:body (http-get-all :sounds)))))
      (is (= 204 (:status (http-delete :sounds 1))))
      (is (= 404 (:status (http-delete :sounds 1))))
      (is (= 1 (count (:body (http-get-all :sounds))))))))


(deftest test-static-resources 
  (let [resp (http/get (url ["test.html"]))]
    (is (= 200 (:status resp)))
    (is (= "Pirate Radio!\n" (:body resp)))))


(deftest test-deploy-endpoint
  (with-redefs [deploy/deploy-program (fn [& args] {:message "hello"})]
    (let [id 876
          resp (http/request {:method :post 
                              :url (url ["deploy-program" id])
                              :as :json})]
      (is (= 200 (:status resp)))
      (is (= {:message "hello"} (:body resp))))))

