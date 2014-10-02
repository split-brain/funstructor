(ns funstructor.command-utils
  (:require [clojure.core.async :as a]
            [cheshire.core :as chs]))

(def server-side-commands
  #{:game-request
    :start-game-ok
    :end-turn
    :action
    :chat-message})

(defn- encode-cmd [command]
  (chs/generate-string command))

(defn- decode-cmd [in-str]
  (try
    (chs/parse-string in-str true)
    (catch java.io.IOException e {:type :decode-error :data in-str})))

(defn branch-channel [ws-channel]
  ;; TODO: Figure out how to transform channel values
  (let [pub (a/pub ws-channel #(:type (decode-cmd %)))]
    {:write-ch ws-channel
     :branches
     (into {}
           (map
            #(do
               (a/sub pub %1 %2)
               [%1 %2])
            server-side-commands
            (repeat (a/chan))))}))

(defn read-cmd [command-type br-ch]
  (a/go
    (a/<! (decode-cmd (get-in br-ch [:branches command-type])))))

(defn write-cmd [cmd br-ch]
  (a/go
    (a/>! (:write-ch br-ch) (encode-cmd cmd))))
