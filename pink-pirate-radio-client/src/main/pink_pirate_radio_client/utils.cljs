(ns pink-pirate-radio-client.utils 
  (:require [clojure.core.async :as async]
            [clojure.string :as string]))


(defn goto!
  [& url-segments]
  (.assign js/window.location (str "#/" (string/join "/" url-segments))))


(defn go-home!
  []
  (goto!))


(defn js-promise->chan
  [js-promise]
  (let [chan (async/chan)]
    (-> js-promise
        (.then #(async/go 
                  (async/>! chan [:success %])
                  (async/close! chan)))
        (.catch #(async/go 
                   (async/>! chan [:error %])
                   (async/close! chan))))
    chan))
