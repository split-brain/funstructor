(ns funstructor.game-state)

(def uuid-chan-map (atom {}))

(defn add-channel [channel uuid]
  (swap! uuid-chan-map assoc uuid channel))
