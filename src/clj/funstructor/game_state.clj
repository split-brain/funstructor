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

(defn get-pending-players [global-state]
  (:pending global-state))

(defn channel-for-uuid [global-state uuid]
  (get-in global-state [:uuid-channel-map uuid]))

(defn uuid-for-channel [global-state channel]
  (get-in global-state [:channel-uuid-map channel]))

(defn get-game [global-state game-id]
  (get-in global-state [:games game-id]))

(defn add-game [global-state game-id game]
  (assoc-in global-state [:games game-id] game))

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
