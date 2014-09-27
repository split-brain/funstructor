(ns funstructor.game-state
  (:require
   [clojure.set :as set]))

(def global-state (atom {:pending #{}
                         :uuid-channel-map {}
                         :games {}}))

;; (defn init-game-state []
;;   (funstructor.commands/pending-checker))

(defn add-channel [channel uuid]
  (swap! global-state update-in [:uuid-channel-map] assoc uuid channel))

(defn add-pending [uuid]
  (swap! global-state update-in [:pending] conj uuid))

(defn pending-players []
  (:pending @global-state))

(defn channel-for-uuid [uuid]
  (get-in @global-state [:uuid-channel-map uuid]))

(defn get-pending-pair []
  (let [pending-players (pending-players)]
    (when (>= 2 (count pending-players))
      [(first pending-players) (second pending-players)])))

(defn remove-from-pending [& uuids]
  (swap! global-state update-in [:pending] set/difference uuids))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; GAME LOGIC
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn gap []
  {:terminal :gap
   :value nil})

(defn make-player-state [opponent]
  {:opponent opponent
   :cards []
   :funstruct [(gap)]})

(defn make-game [p1 p2]
  {:players
   {p1 (make-player-state p2)
    p2 (make-player-state p1)}
   :current-turn p1})

(defn get-opponent-uuid [game uuid]
  (get-in game [:players uuid :opponent]))
