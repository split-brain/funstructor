(ns funstructor.commands
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.core.async :refer [>! <! go go-loop timeout]]

            [funstructor.game-state :refer :all]
            [funstructor.cards :as cards]
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

        (update-global-state (update-game (current-global-state) game-id (make-game uuid1 uuid2)))

        (apply remove-from-pending pending-pair)))
    (recur)))

(defn make-update-data [game-state player-uuid]
  (let [player-state (game-state player-uuid)
        opponent-uuid (get-opponent-uuid game-state player-uuid)]
    (-> (update-in player-state
                   [:cards]
                   (fn [cards]
                     (map cards/cards cards)))
        (assoc :current-turn (:current-turn game-state))
        (assoc :enemy-cards-num (count (:cards (game-state opponent-uuid))))
        (assoc :enemy-funstruct (:funstruct (game-state opponent-uuid))))))

(defmulti handle-command (fn [command channel] (:type command)))
(defmethod handle-command "game-request" [command channel]
  (let [uuid (gen-uuid)]
    (-> (current-global-state)
        (add-channel channel uuid)
        (add-pending uuid)
        (update-global-state))
    (println "Global state: " (current-global-state))
    (send-commands :commands [{:type :game-request-ok
                               :data {:uuid uuid
                                      :pending (pending-players (current-global-state))}}]
                   :channels (map #(channel-for-uuid (current-global-state) %) (pending-players (current-global-state))))))

(defmethod handle-command "start-game-ok" [command channel]
  (let [game-id (get-in [:data :game-id] command)
        game (get-game game-id)
        player-id (uuid-for-channel channel)
        new-game (-> game
                     (mark-player-ready player-id))]
    (update-game (current-global-state) new-game)
    (when (both-players-ready new-game)
      (let [[p1 p2 :as players] (get-game-players new-game)
            [c1 c2] (map #(channel-for-uuid (current-global-state) %) players)]
        (send-commands :commands [{:type :game-update
                                   :data (make-update-data new-game p1)
                                   }]
                       :channels [c1])
        (send-commands :commands [{:type :game-update
                                   :data (make-update-data new-game p2)}]
                       :channels [c2])))))

(defmethod handle-command :default [command channel]
  (printerr "Unrecognized command: " command))




