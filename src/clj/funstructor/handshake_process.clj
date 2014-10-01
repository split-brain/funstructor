(ns funstructor.handshake-process
  (:require [clojure.core.async :as a]

            [funstructor.cards-functions :as f]
            [funstructor.command-utils :as cu]
            [funstructor.utils :as u]))

(defn handshake-process [ws-chan pending-players-chan]
  (a/go
    (let [{:keys [user-name]} (a/<! (cu/wait-for-cmd :game-request ws-chan))
          player-uuid (u/gen-uuid)]
      (a/<! (cu/send-cmd {:type :game-request-ok
                          :uuid player-uuid}
                         ws-chan))
      (a/>! {:ws-chan ws-chan
             :name user-name
             :uuid player-uuid}
            pending-players-chan))))

(def pending-take-delay 5000)

(defn- take-two-random-elements-from-set [set]
  (and (< (count set) 2)
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
          (do (game-process p1 p2)
              (recur (a/timeout pending-take-delay)
                     (-> pending-infos
                         (disj p1)
                         (disj p2))))
          (recur (a/timeout pending-take-delay)
                 pending-infos))

        (recur timeout-chan
               (conj pending-infos))
        ))))

(defn- pre-game-exchange [game-id game-map p1-info p2-info]
  (let [p1-goal (f/get-goal game-map (:uuid p1-info))
        p2-goal (f/get-goal game-map (:uuid p2-info))]
    (go
      (a/<! (cu/send-cmd
             {:type :start-game
              :game-id game-id
              :enemy (f/get-player-name-by-id game-map (:uuid p2-info))
              :goal-name (:name p1-goal)
              :goal-string (:ra2 p1-goal)}
             (:ws-channel p1-info)))
      (a/<! (cu/send-cmd
             {:type :start-game
              :game-id game-id
              :enemy (f/get-player-name-by-id game-map (:uuid p1-info))
              :goal-name (:name p2-goal)
              :goal-string (:ra2 p2-goal)}
             (:ws-channel p2-info)))

      ;; TODO: Assuming that client is good guy
      ;; Rework this in future

      (a/<! (wait-for :start-game-ok (:ws-channel p1-info)))
      (a/<! (wait-for :start-game-ok (:ws-channel p2-info)))
      )))

(defn- game-loop [game-id initial-game-map p1-id p1-chan p2-id p2-chan]
  (a/go-loop [game-map initial-game-map]
    ))

(defn game-process [p1 p2]
  (letfn [(pre-game-exchange [game-id game-map p1-chan p2-chan]
            (a/go
              (a/<! (send-cmd {:type :start-game
                               :enemy (f/get-player-name-by-id game-map )} p1-chan))))]
    (let [game-uuid (u/gen-uuid)
          player1-id (:uuid p1)
          player1-chan (:ws-chan p1)
          player1-name (:name p1)
          player2-id (:uuid p2)
          player2-chan (:ws-chan p2)
          player2-name (:name p2)

          initial-game-map (f/make-game player1-id player2-id player1-name player2-name)]

      (go
        (<! (pre-game-exchange game-id initial-game-map p1 p2)))
      )))
