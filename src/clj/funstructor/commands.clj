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
    (<! (timeout 3000))
    (when-let [pending-pair (get-pending-pair (current-global-state))]
      (let [[uuid1 uuid2] pending-pair
            [channel1 channel2] (map #(channel-for-uuid (current-global-state) %) pending-pair)
            game-id (gen-uuid)]
        (println "Taking two players for game with uuids: " pending-pair "\n\n")
        (send-commands :commands [{:type :start-game
                                   :data {:game-id game-id
                                          :enemy uuid2}}]
                       :channels [channel1])
        (send-commands :commands [{:type :start-game
                                   :data {:game-id game-id
                                          :enemy uuid1}}]
                       :channels [channel2])

        (update-global-state (update-game (current-global-state) game-id (f/make-game uuid1 uuid2)))
        (let [new-state (apply remove-from-pending (current-global-state) pending-pair)]
          (update-global-state new-state))))
    (recur)))

(defn make-update-data [game-state player-uuid]
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
    (-> (current-global-state)
        (add-channel channel uuid)
        (add-pending uuid)
        (update-global-state))
    (println "Processing game-request and generating uuid: " uuid)
    (send-commands :commands [{:type :game-request-ok
                               :data {:uuid uuid
                                      :pending (get-pending-players (current-global-state))}}]
                   :channels [channel]
                   )))

(defmethod handle-command "start-game-ok" [command channel]
  (let [game-id (get-in [:data :game-id] command)
        game (get-game game-id)
        player-id (uuid-for-channel channel)
        new-game (-> game
                     (f/mark-player-ready player-id))]
    (update-game (current-global-state) new-game)
    (when (f/both-players-ready new-game)
      (let [[p1 p2 :as players] (f/get-game-players new-game)
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
