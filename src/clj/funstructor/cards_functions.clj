(ns funstructor.cards-functions
  (:require
   [funstructor.utils :as u]
   [funstructor.cards :as c]))

(defn- apply-to-cards
  "Get access to cards"
  [game-map player-key]
  (partial update-in game-map [player-key :cards]))

(defn- apply-to-funstruct
  "Get access to funstruct"
  [game-map player-key]
  (partial update-in game-map [player-key :funstruct]))

(defn- gap []
  {:terminal :gap})

;; Functions to operate on game state

(def terminal-hierarchy 
  (-> (make-hierarchy)
      (derive :terminal-left-paren :terminal)
      (derive :terminal-right-paren :terminal)
      (derive :terminal-left-square :terminal)
      (derive :terminal-right-square :terminal)
      (derive :terminal-id :terminal-param)
      (derive :terminal-num :terminal-param)))

(defmulti apply-card
  "Modify game-state map and return this map"
  (fn [m p card & args] card)
   :hierarchy #'terminal-hierarchy)

(defmethod apply-card
  :terminal
  [game-map player-key card & args]
  (let [[pos & _] args]
    ((apply-to-funstruct game-map player-key)
     (fn [funstruct]
       (assoc funstruct pos
              {:terminal
               (get-in c/cards [card :value])})))))

(defmethod apply-card
  :terminal-param
  [game-map player-key card & args]
  (let [[pos param & _] args]
    ((apply-to-funstruct game-map player-key)
     (fn [funstruct]
       (assoc funstruct pos
              {:terminal
               (get-in c/cards [card :value])
               :value param})))))

(defmethod apply-card
  :mutator-right-gap
  [game-map player-key card & args]
  ((apply-to-funstruct game-map player-key)
   (fn [funstruct]
     (conj funstruct (gap)))))

(defmethod apply-card
  :mutator-left-gap
  [game-map player-key card & args]
  ((apply-to-funstruct game-map player-key)
   (fn [funstruct]
     (vec (concat [(gap)] funstruct)))))

(defmethod apply-card
  :mutator-position-gap
  [game-map player-key card & args]
  (let [[pos & _] args]
    ((apply-to-funstruct game-map player-key)
     (fn [funstruct]
       (vec (concat (take pos funstruct)
                    [{:type :gap}]
                    (drop pos funstruct)))))))

;; Cards Accessor

(defn- take-card
  "Add card to player state"
  [game-map player-key card]
  ((apply-to-cards game-map player-key)
   (fn [v] (conj v card))))

(defn- delete-card
  "Delete card from player state"
  [game-map player-key card-pos]
  ((apply-to-cards game-map player-key)
   (fn [v] (u/delete-from-vector v card-pos))))







(defn use-card
  "Player Key:  UUID of player
     Card Pos:  index of card in :cards vector
         Args:  additional args specific for function"
  [game-map player-key card-pos & args]
  (-> game-map
      
      ;; apply card
      
      ;; delete card from player
      
      ))
