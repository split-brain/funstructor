(ns funstructor.game-state
  (:require
   [clojure.set :as set]))

(def global-state (atom {:pending #{}
                         :uuid-channel-map {}
                         :channel-uuid-map {}
                         :games {}}))

;; (defn init-game-state []
;;   (funstructor.commands/pending-checker))

(defn current-global-state []
  @global-state)

(defn add-channel [global-state channel uuid]
  (-> global-state
      (update-in [:uuid-channel-map] assoc uuid channel)
      (update-in [:channel-uuid-map] assoc channel uuid)))

(defn add-pending [global-state uuid]
  (update-in global-state [:pending] conj uuid))

(defn pending-players [global-state]
  (:pending global-state))

(defn channel-for-uuid [global-state uuid]
  (get-in global-state [:uuid-channel-map uuid]))

(defn get-pending-pair [global-state]
  (let [pending (pending-players global-state)]
    (when (>= 2 (count pending))
      [(first pending-players) (second pending-players)])))

(defn remove-from-pending [global-state & uuids]
  (update-in global-state [:pending] set/difference uuids))

(defn update-global-state [new-state]
  (swap! global-state (fn [_] new-state)))
