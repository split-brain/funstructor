(ns funstructor.game-state)

(def game-state (atom {:pending #{}
                       :uuid-channel-map {}}))

(defn add-channel [channel uuid]
  (swap! game-state update-in [:uuid-channel-map] assoc channel uuid))

(defn add-pending [uuid]
  (swap! game-state update-in [:pending] conj uuid))

(defn pending-players []
  (:pending @game-state))

(defn channel-for-uuid [uuid]
  (get-in @game-state [:uuid-channel-map uuid]))
