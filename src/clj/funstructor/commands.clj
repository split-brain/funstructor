(ns funstructor.commands
  (:require [cheshire.core :refer [generate-string parse-string]]
            [funstructor.game-state :refer [add-channel]]
            [clojure.core.async :refer [>! go]]
            [funstructor.utils :refer :all]))

(defn send-command [command channel]
  (let [json (generate-string command)]
    (go
      (>! channel json))))

(defn parse-command [json]
  (parse-string json keyword))

(defmulti handle-command (fn [command channel] (:type command)))
(defmethod handle-command :game-request [command channel]
  (let [uuid (gen-uuid)]
    (add-channel channel uuid)
    (send-command {:type :request-ok
                   :data uuid}
                  channel)))

(defmethod handle-command :default [command channel]
  (printerr "Unrecognized command: " command))




