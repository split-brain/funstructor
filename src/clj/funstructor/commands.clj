(ns funstructor.commands
  (:require [cheshire.core :refer [generate-string parse-string]]
            [funstructor.game-state :refer :all]
            [clojure.core.async :refer [>! go]]
            [funstructor.utils :refer :all]))

(defn encode-command [command]
  (generate-string command))

(defn decode-command [in-str]
  (parse-string in-str keyword))

(defn send-command [command & channels]
  (let [json (encode-command command)]
    (doseq [channel channels]
      (go
        (>! channel json)))))

(defmulti handle-command (fn [command channel] (:type command)))
(defmethod handle-command :game-request [command channel]
  (let [uuid (gen-uuid)]
    (add-channel channel uuid)
    (add-pending uuid)
    (send-command {:type :request-ok
                   :uuid uuid
                   :pending (pending-players)}
                  (map channel-for-uuid (pending-players)))))

(defmethod handle-command :default [command channel]
  (printerr "Unrecognized command: " command))




