(ns funstructor.processes
  (:require [clojure.core.async :as a]

            [funstructor.cards-functions :as f]
            [funstructor.commands :as c]
            [funstructor.cards :as cards]
            [funstructor.utils :as u]))

(def turn-time-delay 60000)
(def pending-take-delay 2000)

(def pending-players-chan (a/chan))

(defn handshake-process [ws-chan pending-players-chan]
  (u/log "Starting handshake process")
  (let [br-ch (c/branch-channel ws-chan)]
    (a/go
      (let [cmd (c/read-cmd-by-type br-ch :game-request)
            player-uuid (u/gen-uuid)]

        (u/log "Received game-request. Sending response ...")
        (c/write-cmd-to-ch br-ch {:type :game-request-ok
                                  :data {:uuid player-uuid}})
        (a/>!
         pending-players-chan
         {:br-ch br-ch
          :name (get-in cmd [:data :user-name])
          :uuid player-uuid})))))

(defn- take-two-random-elements-from-set [set]
  (and (>= (count set) 2)
       (let [set-vec (vec set)
             set-count (count set)
             idx1 (rand-int set-count)
             idx2 (rand-int set-count)]
         (if (= idx1 idx2)
           (take-two-random-elements-from-set set)
           [(set-vec idx1) (set-vec idx2)]))))

(declare game-process)

(defn pending-checker-process [pending-players-chan]
  (a/go-loop [timeout-chan (a/timeout pending-take-delay)
              pending-infos #{}]
    (let [[value chan] (a/alts! [pending-players-chan timeout-chan])]
      (condp = chan

        timeout-chan
        (if-let [[p1 p2] (take-two-random-elements-from-set pending-infos)]
          (do
            (game-process p1 p2)
            (recur (a/timeout pending-take-delay)
                   (-> pending-infos
                       (disj p1)
                       (disj p2))))
          (recur (a/timeout pending-take-delay)
                 pending-infos))

        (recur timeout-chan
               (conj pending-infos value))
        ))))

(defn make-player-update-data [game-map player-id]
  (letfn [(fix-board [board]
            (map (fn [board-elem]
                   (update-in board-elem [:key] cards/cards))
                 board))]
    (let [player-state (f/get-player-state game-map player-id)
          opponent-id (f/get-opponent game-map player-id)
          opponent-state (f/get-player-state game-map opponent-id)]
      (-> player-state

          ;; Fixing board's cards
          (update-in
           [:board]
           fix-board)

          ;; Fixing cards in hand
          (update-in
           [:cards]
           (fn [cards]
             (map cards/cards cards)))

          (assoc :enemy-board (f/get-board game-map opponent-id))
          (assoc :enemy-cards-num (count (f/get-cards game-map opponent-id)))
          (assoc :enemy-funstruct (f/get-funstruct game-map opponent-id))))))

(defn make-game-update-data [game-map]
  (select-keys game-map [:messages :win :current-turn]))

(defn make-update-data [game-map player-id]
  (merge (make-game-update-data game-map)
         (make-player-update-data game-map player-id)))

(defn send-game-updates [game-map p1-id p1-ch p2-id p2-ch]
  (a/go
    (let [p1-game-update {:type :game-update
                          :data (make-update-data game-map p1-id)}
          p2-game-update {:type :game-update
                          :data (make-update-data game-map p2-id)}]
      (c/write-cmd-to-ch p1-ch p1-game-update)
      (c/write-cmd-to-ch p2-ch p2-game-update))))


(defmulti apply-cmd (fn [game-map player-id cmd] (:type cmd)))

(defmethod apply-cmd :action [game-map player-id cmd]
  (let [card-idx (get-in cmd [:data :card-idx])
        target (get-in cmd [:data :target])
        value (get-in cmd [:data :value])
        funstr-idx (get-in cmd [:data :funstruct-idx])]
    (f/use-card game-map player-id card-idx funstr-idx value)))

(defmethod apply-cmd :end-turn [game-map player-id cmd]
  (-> game-map
      (f/end-turn-for-player)
      (#(if (f/turn-finished? %)
          (f/end-turn %)
          %))))

(defmulti cmd-resets-timer? :type)

(defmethod cmd-resets-timer? :action [_] false)
(defmethod cmd-resets-timer? :end-turn [_] true)

(defn game-update-process [game-id initial-game-map p1-id p1-ch p2-id p2-ch]
  (u/log "Starting game update process for game" game-id)
  (a/go-loop [game-map initial-game-map
              timer (a/timeout turn-time-delay)]
    (let [current-turn (f/get-current-turn game-map)
          [value ch] (c/read-cmd-from-chs [p1-ch p2-ch]
                                          [:action :end-turn]
                                          :timeout-ch timer)]
      (u/log "Game update process received message: " value)
      (condp = ch

        p1-ch
        (if (= current-turn p1-id)
          (let [new-game-map (apply-cmd game-map p1-id value)]
            (a/<! (send-game-updates new-game-map p1-id p1-ch p2-id p2-ch))
            (recur new-game-map (if (cmd-resets-timer? value)
                                  (a/timeout turn-time-delay)
                                  timer)))
          (recur game-map timer))

        p2-ch
        (if (= current-turn p2-id)
          (let [new-game-map (apply-cmd game-map p2-id value)]
            (a/<! (send-game-updates new-game-map p1-id p1-ch p2-id p2-ch))
            (recur new-game-map (if (cmd-resets-timer? value)
                                  (a/timeout turn-time-delay)
                                  timer)))
          (recur game-map timer))

        timer
        (let [new-game-map (apply-cmd
                            game-map
                            current-turn
                            {:type :end-turn
                             :data {:game-id game-id}})]
          (u/log "Turn time of player" current-turn " has elapsed. Forcing next turn ...")
          (a/<! (send-game-updates new-game-map p1-id p1-ch p2-id p2-ch))
          (recur new-game-map (a/timeout turn-time-delay)))))))


(defn game-chat-process [game-id p1-id p1-ch p2-id p2-ch]
  (u/log "Starting game chat process")
  (a/go-loop []
    (let [[value ch] (c/read-cmd-from-chs [p1-ch p2-ch]
                                          [:chat-message])]
      (u/log "Game chat process received message: " value)
      (when-not (nil? value)
        (condp = ch

          p1-ch
          (do (c/write-cmd-to-chs
               [p1-ch p2-ch]
               {:type :chat-message-response
                :data {:player-id p1-id
                       :message (get-in value [:data :message])
                       }}
               true)
              (recur))

          p2-ch
          (do (c/write-cmd-to-chs
               [p1-ch p2-ch]
               {:type :chat-message-response
                :data {:player-id p2-id
                       :message (get-in value [:data :message])}}
               true)
              (recur)))))))

(defn- pre-game-exchange [game-id game-map p1-info p2-info]
  (u/log "Initiating pre game exchange")
  (let [p1-goal (f/get-goal game-map (:uuid p1-info))
        p2-goal (f/get-goal game-map (:uuid p2-info))]
    (a/go
      (c/write-cmd-to-ch
       (:br-ch p1-info)
       {:type :start-game
        :data {:game-id game-id
               :enemy (f/get-player-name-by-id game-map (:uuid p2-info))
               :goal-name (:name p1-goal)
               :goal-string (:raw p1-goal)}})
      (c/write-cmd-to-ch
       (:br-ch p2-info)
       {:type :start-game
        :data {:game-id game-id
               :enemy (f/get-player-name-by-id game-map (:uuid p1-info))
               :goal-name (:name p2-goal)
               :goal-string (:raw p2-goal)}})

      ;; TODO: Assuming that client is good guy and will send us start-game-ok cmd
      ;; Rework this in future ... with timeouts possibly

      (c/read-cmd-by-type (:br-ch p1-info) :start-game-ok)
      (c/read-cmd-by-type (:br-ch p2-info) :start-game-ok)

      (send-game-updates game-map
                         (:uuid p1-info)
                         (:br-ch p1-info)
                         (:uuid p2-info)
                         (:br-ch p2-info)))))

(defn game-process [p1 p2]
  (u/log "Starting game process")
  (let [game-uuid (u/gen-uuid)
        player1-id (:uuid p1)
        player1-chan (:br-ch p1)
        player1-name (:name p1)
        player2-id (:uuid p2)
        player2-chan (:br-ch p2)
        player2-name (:name p2)

        initial-game-map (f/init-game (f/make-game player1-id player2-id player1-name player2-name))]

    (a/go
      (a/<! (pre-game-exchange game-uuid initial-game-map p1 p2))
      (game-update-process game-uuid
                           initial-game-map
                           player1-id
                           player1-chan
                           player2-id
                           player2-chan)
      (game-chat-process game-uuid
                         player1-id
                         player1-chan
                         player2-id
                         player2-chan))))
