(ns funstructor.cards-functions
  (:require
   [funstructor.utils :as u]
   [funstructor.cards :as c]))

(def start-cards-num 6)
(def cards-per-turn 2)

(defn- gap []
  {:terminal :gap
   :value nil})

(defn- make-player-state [opponent]
  {:ready false
   :opponent opponent
   :cards []
   :funstruct [(gap)]})

(defn make-game [p1 p2]
  {:players
   {p1 (make-player-state p2)
    p2 (make-player-state p1)}
   :current-turn p1
   :turn-ends 0
   :turn 1})

(defn get-game-players [game]
  (keys (:players game)))

(defn mark-player-ready [game player]
  (assoc-in game [:players player :ready] true))

(defn both-players-ready [game]
  (let [[p1 p2] (get-game-players game)]
    (and
     (get-in game [:players p1 :ready])
     (get-in game [:players p2 :ready]))))

(defn get-opponent [game player]
  (get-in game [:players player :opponent]))

(defn turn-finished? [game]
  (= 2 (:turn-ends game)))

(defn get-player-state [game uuid]
  (get-in game [:players uuid]))

(defn get-players [game]
  (keys (get-in game [:players])))

(defn get-cards [game player]
  (get-in game [:players player :cards]))

(defn- apply-to-cards
  "Get access to cards"
  [game-map player-key]
  (partial update-in game-map [:players player-key :cards]))

(defn- apply-to-funstruct
  "Get access to funstruct"
  [game-map player-key]
  (partial update-in game-map [:players player-key :funstruct]))

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

(defn- delete-all-cards
  "Delete all cards from player state"
  [game-map player-key]
  ((apply-to-cards game-map player-key)
   (fn [v] (vec []))))

(defn- get-card [game-map player-key pos]
  (get-in game-map [:players player-key :cards pos]))

(defn end-turn [game-map]
; allow only if two player finished their turn :turn-ends-2
  (let [[p1 p2] (get-players game-map)]
    (-> game-map
        (assoc-in [:turn-ends] 0)
        (take-cards p1 cards-per-turn)
        (take-cards p2 cards-per-turn)
        ;; random card
        (take-card (rand-nth [p1 p2]) (c/next-card))
        ;; increment turn num
        (update-in [:turn] inc)
        )))

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

(defmethod apply-card
  :mutator-shot
  [game-map player-key card & args]
  (let [[pos & _] args]
    ((apply-to-funstruct game-map
                         (get-opponent game-map player-key))
     (fn [funstruct]
       (assoc funstruct pos (gap))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; APPLY-CARD ACTION
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmethod apply-card
  :action-thief-1
  [game-map player-key card & args]
  (let [opponent (get-opponent game-map player-key)
        opp-cards (get-cards game-map opponent)
        opp-cards-num (count opp-cards)]
    (if (> opp-cards-num 0)
      (let [pos (rand-int opp-cards-num)]
        (-> game-map
            ;; first player get cards
            (take-card player-key (get-card game-map opponent pos))
            ;; opponent loses it
            (delete-card opponent pos)))
      game-map)))

(defmethod apply-card
  :action-discard-1
  [game-map player-key card & args]
  (let [opponent (get-opponent game-map player-key)
        opp-cards (get-cards game-map opponent)
        opp-cards-num (count opp-cards)]
    (if (> opp-cards-num 0)
      (let [pos (rand-int opp-cards-num)]
        (-> game-map
            ;; opponent loses random card
            (delete-card opponent pos)))
      game-map)))

(defmethod apply-card
  :action-equality-1
  [game-map player-key card & args]
  (let [opp (get-opponent game-map player-key)]
    (-> game-map
        (delete-all-cards player-key)
        (delete-all-cards opp)
        (take-cards player-key 5)
        (take-cards opp 5))))

(defmethod apply-card
  :action-equality-2
  [game-map player-key card & args]
  (let [opp (get-opponent game-map player-key)
        opp-cards (get-cards game-map opp)
        your-cards-num (count (get-cards game-map player-key))
        opp-cards-num (count opp-cards)]
    (if (< your-cards-num opp-cards-num)
      (take-cards game-map player-key (- opp-cards-num your-cards-num))
      game-map)))

(defmethod apply-card
  :action-equality-3
  [game-map player-key card & args]
  (let [opp (get-opponent game-map player-key)]
    (-> game-map
        (delete-all-cards player-key)
        (delete-all-cards opp))))


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

(defn end-turn-for-player [game]
  (-> game
      (assoc :current-turn (get-opponent game (:current-turn game)))
      (update-in [:turn-ends] inc)))

(defn init-game [game-map]
  (let [players (get-players game-map)]
    (reduce (fn [m player]
              (take-cards m player start-cards-num))
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
