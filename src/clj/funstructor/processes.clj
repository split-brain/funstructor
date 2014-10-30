(ns funstructor.processes
  (:require [clojure.core.async :as a]
            [taoensso.timbre :as t]

            [funstructor.cards-functions :as f]
            [funstructor.commands :as c]
            [funstructor.cards :as cards]
            [funstructor.utils :as u]))

(def turn-time-delay 60000)
(def pending-take-delay 2000)

(defn handshake-process [ws-chan pending-players-chan]
  (t/info "Starting handshake process")
  (let [br-ch (c/branch-channel ws-chan)]
    (a/go
      (let [cmd (c/read-cmd-by-type br-ch :game-request)
            player-id (u/next-id!)]

        (t/info "Received game-request from player" player-id ". Sending response ...")
        (c/write-cmd-to-ch br-ch {:type :game-request-ok
                                  ;; TODO: Replace uuid with id on client side
                                  :data {:uuid player-id}})
        (a/>!
         pending-players-chan
         {:br-ch br-ch
          :name (get-in cmd [:data :user-name])
          :id player-id})))))

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

        (when-not (nil? value)
          (recur timeout-chan
                 (conj pending-infos value)))
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

(defn send-game-updates [game-map p1-info p2-info]
  (a/go
    (let [p1-game-update {:type :game-update
                          :data (make-update-data game-map (:id p1-info))}
          p2-game-update {:type :game-update
                          :data (make-update-data game-map (:id p2-info))}]
      (c/write-cmd-to-ch (:br-ch p1-info) p1-game-update)
      (c/write-cmd-to-ch (:br-ch p2-info) p2-game-update))))


(defmulti apply-cmd (fn [game-map player-id cmd] (:type cmd)))

(defmethod apply-cmd :action [game-map player-id cmd]
  (let [{:keys [card-idx target value funstruct-idx]} (:data cmd)]
    (f/use-card game-map player-id card-idx funstruct-idx value)))

(defmethod apply-cmd :end-turn [game-map player-id cmd]
  (-> game-map
      (f/end-turn-for-player)
      (#(if (f/turn-finished? %)
          (f/end-turn %)
          %))))

(defmulti cmd-resets-timer? :type)

(defmethod cmd-resets-timer? :action [_] false)
(defmethod cmd-resets-timer? :end-turn [_] true)



(defn- handle-command [sender-id cmd game-map timer]
  (if (= (f/get-current-turn game-map) sender-id)
    (let [new-game-map (apply-cmd game-map sender-id cmd)]
      [new-game-map (if (cmd-resets-timer? cmd)
                      (a/timeout turn-time-delay)
                      timer)])
    [game-map timer]))

(defn game-update-process [game-id game-state-ch initial-game-map p1-info p2-info]
  (t/info "Starting game update process for game" game-id)
  (let [{p1-id :id p1-ch :br-ch} p1-info
        {p2-id :id p2-ch :br-ch} p2-info]
    (a/go-loop [game-map initial-game-map
                timer (a/timeout turn-time-delay)]
      (let [[value ch] (c/read-cmd-from-chs [p1-ch p2-ch]
                                            [:action :end-turn]
                                            :timeout-ch timer)]

        (t/info "Game update process received message: " value)

        (if (and (not (= ch timer))
                 (nil? value))
          (let [new-game-map (f/player-win game-map
                                           (if (= ch p1-ch)
                                             p2-id
                                             p1-id))]
            (a/>! game-state-ch new-game-map))

          (condp = ch

            p1-ch
            (let [[new-game-map timer] (handle-command p1-id value game-map timer)]
              (a/>! game-state-ch new-game-map)
              (recur new-game-map timer))

            p2-ch
            (let [[new-game-map timer] (handle-command p2-id value game-map timer)]
              (a/>! game-state-ch new-game-map)
              (recur new-game-map timer))

            timer
            (let [[new-game-map timer] (handle-command (f/get-current-turn game-map)
                                                       {:type :end-turn
                                                        :data {:game-id game-id}}
                                                       timer)]
              (t/info "Turn time of player" (f/get-current-turn game-map) "has elapsed. Forcing next turn ...")
              (a/>! game-state-ch new-game-map)
              (recur new-game-map timer))))))))


(defn game-chat-process [game-id p1-info p2-info]
  (t/info "Starting game chat process for game " game-id)
  (let [{p1-name :name p1-ch :br-ch} p1-info
        {p2-name :name p2-ch :br-ch} p2-info]
    (a/go-loop []
      (let [[value ch] (c/read-cmd-from-chs [p1-ch p2-ch]
                                            [:chat-message])]
        (t/info "Game chat process received message: " value)
        (when-not (nil? value)
          (condp = ch

            p1-ch
            (do (c/write-cmd-to-chs
                 [p1-ch p2-ch]
                 {:type :chat-message-response
                  :data {:player-id p1-name
                         :message (get-in value [:data :message])
                         }}
                 true)
                (recur))

            p2-ch
            (do (c/write-cmd-to-chs
                 [p1-ch p2-ch]
                 {:type :chat-message-response
                  :data {:player-id p2-name
                         :message (get-in value [:data :message])}}
                 true)
                (recur))))))))

(defn- pre-game-exchange [game-id game-map p1-info p2-info]
  (t/info "Initiating pre game exchange for game" game-id)
  (let [p1-goal (f/get-goal game-map (:id p1-info))
        p2-goal (f/get-goal game-map (:id p2-info))]
    (a/go
      (c/write-cmd-to-ch
       (:br-ch p1-info)
       {:type :start-game
        :data {:game-id game-id
               :enemy (f/get-player-name-by-id game-map (:id p2-info))
               :goal-name (:name p1-goal)
               :goal-string (:raw p1-goal)}})
      (c/write-cmd-to-ch
       (:br-ch p2-info)
       {:type :start-game
        :data {:game-id game-id
               :enemy (f/get-player-name-by-id game-map (:id p1-info))
               :goal-name (:name p2-goal)
               :goal-string (:raw p2-goal)}})

      ;; TODO: Assuming that client is good guy and will send us start-game-ok cmd
      ;; Rework this in future ... with timeouts possibly

      (c/read-cmd-by-type (:br-ch p1-info) :start-game-ok)
      (c/read-cmd-by-type (:br-ch p2-info) :start-game-ok))))

(defn game-state-emitter-process [game-id game-state-ch p1-info p2-info]
  (a/go-loop [game-state (a/<! game-state-ch)]
    (when game-state
      (send-game-updates game-state p1-info p2-info)
      (recur (a/<! game-state-ch)))))

(defn game-process [p1-info p2-info]
  (let [game-id (u/next-id!)
        game-state-ch (a/chan) ;; TODO: Consider using sliding buffer for this chan

        initial-game-map (f/init-game (f/make-gameme
                                       (:id p1-info)
                                       (:id p2-info)
                                       (:name p1-info)
                                       (:name p2-info)))]


    (t/info "Starting game process for players" (:id p1-info) (:id p2-info) "and game" game-id)
    (a/go
      (game-state-emitter-process game-id game-state-ch p1-info p2-info)
      (a/<! (pre-game-exchange game-id initial-game-map p1-info p2-info))
      (a/>! game-state-ch initial-game-map)
      (game-update-process game-id
                           game-state-ch
                           initial-game-map
                           p1-info
                           p2-info)
      (game-chat-process game-id
                         p1-info
                         p2-info))))

