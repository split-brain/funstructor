(ns funstructor.commands
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.core.async :refer [>! <! go go-loop timeout]]

            [funstructor.game-state :refer :all]
            [funstructor.cards :as cards]
            [funstructor.cards-functions :as f]
            [funstructor.utils :as u]))

(defn encode-command [command]
  (generate-string command))

(defn decode-command [in-str]
  (try
    (parse-string in-str true)
    (catch java.io.IOException e {:type :decode-error :data in-str})))

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
            game-id (u/gen-uuid)]
        (println "Taking two players for game with uuids: " pending-pair "\n\n")
        (send-commands :commands [{:type :start-game
                                   :data {:game-id game-id
                                          :enemy uuid2}}]
                       :channels [channel1])
        (send-commands :commands [{:type :start-game
                                   :data {:game-id game-id
                                          :enemy uuid1}}]
                       :channels [channel2])

        (update-global-state add-game game-id (f/make-game uuid1 uuid2))
        (update-global-state #(apply remove-from-pending % pending-pair))))
    (recur)))

(defn make-update-data [game-state player-uuid]
  (let [player-state (f/get-player-state game-state player-uuid)
        opponent-uuid (f/get-opponent game-state player-uuid)]
    (-> (update-in player-state
                   [:cards]
                   (fn [cards]
                     (map cards/cards cards)))
        (assoc :current-turn (:current-turn game-state))
        (assoc :enemy-cards-num (count (:cards (f/get-player-state game-state opponent-uuid))))
        (assoc :enemy-funstruct (:funstruct (f/get-player-state game-state opponent-uuid))))))

(defn send-game-updates [game-id]
  (let [initialized-game (get-game (current-global-state) game-id)
        [p1 p2 :as players] (f/get-game-players initialized-game)
        [c1 c2] (map #(channel-for-uuid (current-global-state) %) players)]
    (u/log "Sending game-update's for game " game-id)
    (send-commands :commands [{:type :game-update
                               :data (make-update-data initialized-game p1)
                               }]
                   :channels [c1])
    (send-commands :commands [{:type :game-update
                               :data (make-update-data initialized-game p2)}]
                   :channels [c2])))

(defmulti handle-command (fn [command channel] (:type command)))

(defmethod handle-command "game-request" [command channel]
  (let [uuid (u/gen-uuid)]
    (update-global-state (comp #(add-channel % channel uuid)
                               #(add-pending % uuid)))
    (u/log "Processing game-request and generating uuid: " uuid)
    (send-commands :commands [{:type :game-request-ok
                               :data {:uuid uuid
                                      :pending (get-pending-players (current-global-state))}}]
                   :channels [channel]
                   )))

(defmethod handle-command "start-game-ok" [command channel]
  (let [game-id (u/uuid-from-string (get-in command [:data :game-id]))
        game (get-game (current-global-state) game-id)
        player-id (uuid-for-channel (current-global-state) channel)
        ]
    (u/log "Processing start-game-ok for game: " game-id
           "\n\n")
    (update-global-state update-game game-id f/mark-player-ready player-id)
    (when (f/both-players-ready (get-game (current-global-state) game-id))
      (update-global-state update-game game-id f/init-game)
      (send-game-updates game-id))))

(defmethod handle-command "action" [command channel]
  (let [game-id (u/uuid-from-string (get-in command [:data :game-id]))
        player-id (uuid-for-channel (current-global-state) channel)
        card-idx (get-in command [:data :card-idx])
        target (get-in command [:data :target])
        funstr-idx (get-in command [:data :funstruct-idx])]
    (update-global-state update-game game-id f/use-card player-id card-idx funstr-idx)
    (send-game-updates game-id)))

(defmethod handle-command "end-turn" [command channel]
  (let [game-id (u/uuid-from-string (get-in command [:data :game-id]))
        player-id (uuid-for-channel (current-global-state) channel)]

    (u/log "Processing end-turn for game: " game-id)

    (update-global-state update-game game-id f/end-turn-for-player)
    (let [updated-game (get-game (current-global-state) game-id)]
      (when (f/turn-finished? updated-game)
        (update-global-state update-game game-id f/end-turn)
        (send-game-updates game-id)))))

(defmethod handle-command :default [command channel]
  (u/printerr "Unrecognized command: " command))
