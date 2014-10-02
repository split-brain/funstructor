(ns funstructor.handshake-process
  (:require [clojure.core.async :as a]

            [funstructor.cards-functions :as f]
            [funstructor.command-utils :as cu]
            [funstructor.utils :as u]))

(def turn-time-delay 60000)
(def pending-take-delay 5000)

(defn handshake-process [ws-chan pending-players-chan]
  (let [br-ch (cu/branch-channel ws-chan)]
    (a/go
      (let [{:keys [user-name]} (a/<! (cu/read-cmd :game-request br-ch))
            player-uuid (u/gen-uuid)]
        (a/<! (cu/write-cmd {:type :game-request-ok
                             :uuid player-uuid}
                            br-ch))
        (a/>! {:br-ch br-ch
               :name user-name
               :uuid player-uuid}
              pending-players-chan)))))

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
    (a/go
      (a/<! (cu/write-cmd
             {:type :start-game
              :game-id game-id
              :enemy (f/get-player-name-by-id game-map (:uuid p2-info))
              :goal-name (:name p1-goal)
              :goal-string (:ra2 p1-goal)}
             (:br-ch p1-info)))
      (a/<! (cu/write-cmd
             {:type :start-game
              :game-id game-id
              :enemy (f/get-player-name-by-id game-map (:uuid p1-info))
              :goal-name (:name p2-goal)
              :goal-string (:ra2 p2-goal)}
             (:br-ch p2-info)))

      ;; TODO: Assuming that client is good guy and will send us start-game-ok cmd
      ;; Rework this in future ... with timeouts possibly

      (a/<! (cu/read-cmd :start-game-ok (:br-ch p1-info)))
      (a/<! (cu/read-cmd :start-game-ok (:br-ch p2-info))))))

(defn- chat-loop [])

(defn- game-loop [game-id initial-game-map p1-id p1-chan p2-id p2-chan]
  (let [p1-pub (a/pub p1-chan)
        p2-pub (a/pub p2-chan)]
      (a/go-loop [timeout-chan (a/timeout turn-time-delay)
                  game-map initial-game-map]
        )))

(defn game-process [p1 p2]
  (let [game-uuid (u/gen-uuid)
        player1-id (:uuid p1)
        player1-chan (:br-ch p1)
        player1-name (:name p1)
        player2-id (:uuid p2)
        player2-chan (:br-ch p2)
        player2-name (:name p2)

        initial-game-map (f/make-game player1-id player2-id player1-name player2-name)]

    (a/go
      (a/<! (pre-game-exchange game-uuid initial-game-map p1 p2))
      (game-loop game-uuid
                 initial-game-map
                 player1-id
                 player1-chan
                 player2-id
                 player2-chan))))

