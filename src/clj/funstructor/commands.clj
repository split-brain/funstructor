(ns funstructor.commands
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.core.async :refer [>! <! go go-loop timeout]]

            [funstructor.game-state :as gs]
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

(defn send-command [command channel]
  (send-commands :commands [command] :channels [channel]))

(defn send-command-by-channels [command & channels]
  (send-commands :commands [command] :channels channels))

(defn pending-checker []
  (go-loop []
    (<! (timeout 3000))
    (when-let [pending-pair (gs/get-pending-pair (gs/current-global-state))]
      (let [[uuid1 uuid2] pending-pair
            [channel1 channel2] (map #(gs/channel-for-player (gs/current-global-state) %) pending-pair)
            game-id (u/gen-uuid)]
        (gs/update-global-state (comp
                                 #(gs/add-game % game-id (f/make-game uuid1 uuid2))
                                 #(apply gs/remove-from-pending % pending-pair)))

        (u/log "Taking two players for game " game-id " with uuids: " pending-pair)
        (let [added-game (gs/get-game (gs/current-global-state) game-id)
              p1-goal (f/get-goal added-game uuid1)
              p2-goal (f/get-goal added-game uuid2)]
          (send-command {:type :start-game
                               :data {:game-id game-id
                                      :enemy (gs/get-player-name (gs/current-global-state) uuid2)
                                      :goal-name (:name p1-goal)
                                      :goal-string (:raw p1-goal)}}
                              channel1)
          (send-command {:type :start-game
                         :data {:game-id game-id
                                :enemy (gs/get-player-name (gs/current-global-state) uuid1)
                                :goal-name (:name p2-goal)
                                :goal-string (:raw p2-goal)}}
                        channel2))
        ))
    (recur)))

(defn make-player-update-data [game-state player-uuid]
  (let [player-state (f/get-player-state game-state player-uuid)
        opponent-uuid (f/get-opponent game-state player-uuid)]
    (-> (update-in player-state
                   [:cards]
                   (fn [cards]
                     (map cards/cards cards)))
        (assoc :current-turn (:current-turn game-state))
        (assoc :enemy-cards-num (count (:cards (f/get-player-state game-state opponent-uuid))))
        (assoc :enemy-funstruct (:funstruct (f/get-player-state game-state opponent-uuid))))))

(defn make-game-update-data [game]
  (select-keys game [:messages]))

(defn make-update-data [game player]
  (merge (make-game-update-data game)
         (make-player-update-data game player)))

(defn send-game-updates [game-id]
  (let [game (gs/get-game (gs/current-global-state) game-id)
        [p1 p2 :as players] (f/get-game-players game)
        [c1 c2] (map #(gs/channel-for-player (gs/current-global-state) %) players)]
    (u/log "Sending game-update's for game " game-id)
    (send-command {:type :game-update
                   :data (make-update-data game p1)}
                   c1)
    (send-command {:type :game-update
                    :data (make-update-data game p2)}
                   c2)))


;; Functions for command handling


(defmulti handle-command (fn [command channel] (:type command)))

(defmethod handle-command "game-request" [command channel]
  (let [uuid (u/gen-uuid)
        user-name (get-in command [:data :user-name])]
    (gs/update-global-state (comp
                             #(gs/add-player-name % uuid user-name)
                             #(gs/add-channel % channel uuid)
                             #(gs/add-pending % uuid)))
    (u/log "Processing game-request and generating uuid for player: " uuid)
    (send-command {:type :game-request-ok
                   :data {:uuid uuid
                          :pending (gs/get-pending-players (gs/current-global-state))}}
                  channel)))

(defmethod handle-command "start-game-ok" [command channel]
  (let [game-id (u/uuid-from-string (get-in command [:data :game-id]))
        game (gs/get-game (gs/current-global-state) game-id)
        player-id (gs/player-for-channel (gs/current-global-state) channel)
        ]
    (u/log "Processing start-game-ok for game: " game-id)
    (gs/update-global-state gs/update-game game-id f/mark-player-ready player-id)
    (when (f/both-players-ready (gs/get-game (gs/current-global-state) game-id))
      (gs/update-global-state gs/update-game game-id f/init-game)
      (send-game-updates game-id))))

(defmethod handle-command "action" [command channel]
  (let [game-id (u/uuid-from-string (get-in command [:data :game-id]))
        player-id (gs/player-for-channel (gs/current-global-state) channel)
        card-idx (get-in command [:data :card-idx])
        target (get-in command [:data :target])
        funstr-idx (get-in command [:data :funstruct-idx])]
    (gs/update-global-state gs/update-game game-id f/use-card player-id card-idx funstr-idx)
    (send-game-updates game-id)))

(defmethod handle-command "end-turn" [command channel]
  (let [game-id (u/uuid-from-string (get-in command [:data :game-id]))
        player-id (gs/player-for-channel (gs/current-global-state) channel)]

    (u/log "Processing end-turn for game: " game-id)

    (gs/update-global-state gs/update-game game-id f/end-turn-for-player)
    (send-game-updates game-id)
    (let [updated-game (gs/get-game (gs/current-global-state) game-id)]
      (when (f/turn-finished? updated-game)
        (gs/update-global-state gs/update-game game-id f/end-turn)
        (send-game-updates game-id)))))

(defmethod handle-command "chat-message" [command channel]
  (let [game-id (u/uuid-from-string (get-in command [:data :game-id]))
        message (get-in command [:data :message])
        player-id (gs/player-for-channel (gs/current-global-state) channel)]
    (send-command {:type :chat-message-response
                   :data {:player-id player-id
                          :message message}}
                  (map #(gs/channel-for-player (gs/current-global-state) %)
                       (f/get-game-players (gs/get-game (gs/current-global-state) game-id))))))

(defmethod handle-command :default [command channel]
  (u/printerr "Unrecognized command: " command))
