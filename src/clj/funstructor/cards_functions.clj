(ns funstructor.cards-functions
  (:require
   [funstructor.utils :as u]
   [funstructor.cards :as c]))

(defn- gap []
  {:terminal :gap
   :value nil})

(defn- make-player-state [opponent]
  {:opponent opponent
   :cards []
   :funstruct [(gap)]})

(defn make-game [p1 p2]
  {:players
   {p1 (make-player-state p2)
    p2 (make-player-state p1)}
   :current-turn p1})

(defn get-game-players [game]
  (keys (:players game)))

(defn mark-player-ready [game player]
  (assoc-in [:players player :ready] true))

(defn both-players-ready [game]
  (let [[p1 p2] (get-game-players game)]
    (and
     (get-in game [:players p1 :ready])
     (get-in game [:players p2 :ready]))))

(defn get-opponent-uuid [game uuid]
  (get-in game [:players uuid :opponent]))

(defn get-player-state [game uuid]
  (get-in game [:players uuid]))

(defn- apply-to-cards
  "Get access to cards"
  [game-map player-key]
  (partial update-in game-map [:players player-key :cards]))

(defn- apply-to-funstruct
  "Get access to funstruct"
  [game-map player-key]
  (partial update-in game-map [:players player-key :funstruct]))

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
                    [(gap)]
                    (drop pos funstruct)))))))

;; Cards Accessor

(defn- take-card
  "Add card to player state"
  [game-map player-key card]
  ((apply-to-cards game-map player-key)
   (fn [v] (conj v card))))

(defn take-cards [game-map player-key num]
  (reduce
   (fn [m e] (take-card m player-key e))
   game-map
   (repeatedly num c/next-card)))

(defn- delete-card
  "Delete card from player state"
  [game-map player-key card-pos]
  ((apply-to-cards game-map player-key)
   (fn [v] (u/delete-from-vector v card-pos))))

(defn- get-card [game-map player-key pos]
  (get-in game-map [:players player-key :cards pos]))

(defn end-turn-for-player [game-map player-key])

(defn end-turn [game-map])


(defn use-card
  "Player Key:  UUID of player
     Card Pos:  index of card in :cards vector
         Args:  additional args specific for function"
  [game-map player-key card-pos & args]
  (-> game-map
      ;; TODO validate such card is present
      (#(apply apply-card % player-key (get-card game-map player-key card-pos) args))
      ;; delete card from player
      (delete-card player-key card-pos)
      
      ))

(defn init-game [game-map]
  (let [players (keys (get-in game-map [:players]))]
    (reduce (fn [m player]
              (take-cards m player 6))
            game-map
            players)))

;; Examples
;; Use :terminal
;; (use-card :g :p 0 :position-in-funstruct)
;; Use :terminal-param
;; (use-card :g :p 0 :position-in-funstruct value)
;; Use :mutator-left-paren, :mutator-right:paren
;; (use-card :g :p 0)
;; Use :mutator-position-gap
;; (use-card :g :p 0 :position-after-in-funstruct)
