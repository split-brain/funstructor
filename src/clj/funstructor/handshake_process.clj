(ns funstructor.handshake-process
  (:require [clojure.core.async :as a]
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

(defn pending-checker-process [pending-players-chan]
  (a/go-loop [timeout-chan (timeout pending-take-delay)
              pending-infos #{}]
    (let [[value chan] (a/alts! [pending-players-chan timeout-chan])]
      (condp = chan

        timeout-chan
        (if-let [[p1 p2] (take-two-random-elements-from-set pending-infos)]
          (do (game-process p1 p2)
              (recur (timeout pending-take-delay)
                     (-> pending-infos
                         (disj p1)
                         (disj p2))))
          (recur (timeout pending-take-delay)
                 pending-infos))

        (recur timeout-chan
               (conj pending-infos))
        ))))


(defn game-process [p1 p2]
  (letfn [(pre-game-exchange [game-id game-map p1-chan p2-chan]
            (a/go
              (a/<! (send-cmd {:type :start-game
                               :enemy } p1-chan))))]
    (let [game-uuid (u/gen-uuid)
          player1-id (:uuid p1)
          player1-chan (:ws-chan p1)
          player2-id (:uuid p2)
          player2-chan (:ws-chan p2)]
      )))
