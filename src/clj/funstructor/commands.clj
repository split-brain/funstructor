(ns funstructor.commands
  (:require [cheshire.core :refer [generate-string parse-string]]
            [funstructor.game-state :refer :all]
            [clojure.core.async :refer [>! <! go go-loop timeout]]
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

(defn pending-checker []
  (go-loop []
    (<! (timeout 1000))
    (when-let [pending-pair (get-pending-pair)]
      (let [[uuid1 uuid2] pending-pair
            [channel1 channel2] (map channel-for-uuid pending-pair)]
        (send-command {:type :start-game
                       :enemy uuid2}
                      channel1)
        (send-command {:type :start-game
                       :enemy uuid1}
                      channel2)
        (apply remove-from-pending pending-pair)))
    (recur)))

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




