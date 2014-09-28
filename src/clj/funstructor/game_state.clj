(ns funstructor.game-state
  (:require
   [clojure.set :as set]
   [funstructor.cards-functions :as f]))

(defn make-global-state []
  {:pending #{}
   :player-channel-map {}
   :channel-player-map {}
   :player-name-map {}
   :player-game-map {}
   :games {}})

(def global-state (atom (make-global-state)))

(defn current-global-state []
  @global-state)

(defn add-player-name [global-state player name]
  (update-in global-state [:player-name-map] assoc player name))

(defn get-player-name [global-state player]
  (get-in global-state [:player-name-map player]))

(defn get-player-game [global-state player]
  (get-in global-state [:player-game-map player]))

(defn add-channel [global-state channel uuid]
  (-> global-state
      (update-in [:player-channel-map] assoc uuid channel)
      (update-in [:channel-player-map] assoc channel uuid)))

(defn add-pending [global-state uuid]
  (update-in global-state [:pending] conj uuid))

(defn get-pending-players [global-state]
  (:pending global-state))

(defn channel-for-player [global-state uuid]
  (get-in global-state [:player-channel-map uuid]))

(defn player-for-channel [global-state channel]
  (get-in global-state [:channel-player-map channel]))

(defn get-game [global-state game-id]
  (get-in global-state [:games game-id]))

(defn add-game [global-state game-id game]
  (let [[p1 p2] (f/get-game-players game)]
    (-> global-state
              (assoc-in [:games game-id] game)
              (assoc-in [:player-game-map p1] game-id)
              (assoc-in [:player-game-map p2] game-id))))

(defn update-game [global-state game-uuid update-func & args]
  (apply update-in global-state [:games game-uuid] update-func args))

(defn get-pending-pair [global-state]
  (let [pending (get-pending-players global-state)]
    (when (>= (count pending) 2)
      [(first pending) (second pending)])))

(defn remove-from-pending [global-state & uuids]
  (update-in global-state [:pending] set/difference uuids))

(defn update-global-state [f & args]
  (apply swap! global-state f args))
