(ns funstructor.commands
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.core.async :refer [>! <! go go-loop timeout]]

            [funstructor.game-state :refer :all]
            [funstructor.cards :as cards]
            [funstructor.cards-functions :as f]
            [funstructor.utils :refer :all]))

(defn encode-command [command]
  (generate-string command))

(defn decode-command [in-str]
  (parse-string in-str true))

(defn send-commands [& {:keys [commands channels]}]
  (doseq [channel channels]
    (go (doseq [command commands]
          (let [json (encode-command command)]
            (>! channel json))))))

(defn pending-checker []
  (go-loop []
    (<! (timeout 1000))
    (when-let [pending-pair (get-pending-pair)]
      (let [[uuid1 uuid2] pending-pair
            [channel1 channel2] (map channel-for-uuid pending-pair)
            game-id (gen-uuid)]

        (send-commands :commands [{:type :start-game
                                   :game-id game-id
                                   :enemy uuid2}]
                       :channels channel1)
        (send-commands :commands [{:type :start-game
                                   :game-id game-id
                                   :enemy uuid1}]
                       :channels channel2)

        (update-global-state (add-game (current-global-state) game-id (f/make-game uuid1 uuid2)))

        (apply remove-from-pending pending-pair)))
    (recur)))

(defn make-update-info [player-uuid game-state]
  (let [player-state (f/get-player-state player-uuid)
        opponent-uuid (f/get-opponent-uuid game-state player-uuid)]
    (-> (update-in player-state
                   [:cards]
                   (fn [cards]
                     (map cards/cards cards)))
        (assoc :current-turn (:current-turn game-state))
        (assoc :enemy-cards-num (count (:cards (f/get-player-state opponent-uuid))))
        (assoc :enemy-funstruct (:funstruct (f/get-player-state opponent-uuid))))))

(defmulti handle-command (fn [command channel] (:type command)))
(defmethod handle-command "game-request" [command channel]
  (let [uuid (gen-uuid)]
    (add-channel channel uuid)
    (add-pending uuid)
    (-> (current-global-state)
        (add-channel channel uuid)
        (add-pending)
        (update-global-state))
    (println "Global state: " (current-global-state))
    (send-commands :commands [{:type :game-request-ok
                               :data {:uuid uuid
                                      :pending (pending-players (current-global-state))}}]
                   :channels (map channel-for-uuid (pending-players (current-global-state))))))

(defmethod handle-command "start-game-ok" [command channel]
  (let [game-id (get-in [:data :game-id] command)
        player-id (uuid-for-channel channel)]
    ))

(defmethod handle-command :default [command channel]
  (printerr "Unrecognized command: " command))
