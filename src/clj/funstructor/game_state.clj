(ns funstructor.game-state
  (:require
   funstructor.commands
   [clojure.set :as set]))

(def game-state (atom {:pending #{}
                       :uuid-channel-map {}}))

(defn init-game-state []
  (funstructor.commands/pending-checker))

(defn add-channel [channel uuid]
  (swap! game-state update-in [:uuid-channel-map] assoc channel uuid))

(defn add-pending [uuid]
  (swap! game-state update-in [:pending] conj uuid))

(defn pending-players []
  (:pending @game-state))

(defn channel-for-uuid [uuid]
  (get-in @game-state [:uuid-channel-map uuid]))

(defn get-pending-pair []
  (let [pending-players (pending-players)]
    (when (>= 2 (count pending-players))
      [(first pending-players) (second pending-players)])))

(defn remove-from-pending [& uuids]
  (swap! game-state update-in [:pending] set/difference uuids))
