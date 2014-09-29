(ns funstructor.cards-functions
  (:require
   [funstructor.utils :as u]
   [funstructor.funstruct-base :as base]
   [funstructor.cards :as c]))

(def start-cards-num 6)
(def card-limit-on-hand 10)
(def cards-per-turn 2)

(defn log-message [game message]
  (update-in game [:messages] (fn [v] (conj v message))))

(defn- delete-all-messages
  [game]
  (update-in game [:messages] (fn [v] (vec []))))


(defn get-player-name-by-id [game player]
  (get-in game [:player-uuid-map player]))

(defn- gap []
  {:terminal :gap
   :value nil})

(defn- make-player-state [goal opponent]
  {:ready false
   :opponent opponent
   :cards []
   :funstruct [(gap)]
   :goal goal
   :board []})

(defn make-game [p1 p2]
  (->
   {:players
    (let [[f1 f2] (base/get-two-random-funstructs)]
      {p1 (make-player-state f1 p2)
       p2 (make-player-state f2 p1)})
    :current-turn p1
    :turn-ends 0
    :turn 1
    :messages []
    :player-uuid-map {} ;; uuid -> name
    :win nil}
))

(defn player-win [game player & reason]
  (-> game
      (assoc :win player)
      (log-message (or reason ""))))

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

(defn get-funstruct [game player]
  (get-in game [:players player :funstruct]))

(defn get-board [game player]
  (get-in game [:players player :board]))

(defn get-goal [game player]
  (get-in game [:players player :goal]))

(defn- apply-to-cards
  "Get access to cards"
  [game-map player-key]
  (partial update-in game-map [:players player-key :cards]))


(defn- apply-to-funstruct
  "Get access to funstruct"
  [game-map player-key]
  (partial update-in game-map [:players player-key :funstruct]))

(defn- apply-to-board
  "Get access to cards"
  [game-map player-key]
  (partial update-in game-map [:players player-key :board]))


;; Cards Accessor

(defn- take-card
  "Add card to player state"
  [game-map player-key card]
  ((apply-to-cards game-map player-key)
   (fn [v]
     (if (< (count v) card-limit-on-hand)
       (conj v card) ;; just add card
       (-> v
           (u/delete-from-vector (rand-int (count v)))
           (conj card))))))

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

(defn end-turn-for-player [game]
  (-> game
      (delete-all-messages)
      (assoc :current-turn (get-opponent game (:current-turn game)))
      (#(log-message % (str (get-player-name-by-id % (:current-turn %))" moves")))
      (update-in [:turn-ends] inc)))

(defn- decrement-duration-for-player [game player]
  ((apply-to-board game player)
   (fn [b]
     (->> b
          (mapv (fn [c]
                  (update-in c [:turns-left] dec))) ;; decrement turns
          (filterv (fn [c]
                     (> (get-in c [:turns-left]) 0)))))))

(defn- decrement-durations [game]
  (let [[p1 p2] (get-game-players game)]
    (-> game
        (decrement-duration-for-player p1)
        (decrement-duration-for-player p2)
        )))

(defn init-game [game-map]
  (let [players (get-players game-map)]
    (-> (reduce (fn [m player]
                 (take-cards m player start-cards-num))
               game-map
               players)
        (log-message (str "Turn: " (:turn game-map))))))

(defn- get-luck-cards-chances [game player]
  (let [chances {:duration-luck-1 75
                 :duration-luck-2 100}]
    (->> (get-board game player)
         (mapv :key)
         (filterv (fn [e] (contains? chances e)))
         (mapv chances))))

;; TODO rewrite it's awful
(defn third-card-winner [game]
  (let [[p1 p2] (get-players game)
        p1-luck-cards (get-luck-cards-chances game p1)
        p2-luck-cards (get-luck-cards-chances game p2)]
    (let [p1max (apply max 0 p1-luck-cards)
          p2max (apply max 0 p2-luck-cards)]
      (cond
       (= p1max p2max) (rand-nth [p1 p2])

       (> p1max p2max)
       (let [r (rand-int 100)]
         (if (<= r p1max) p1 p2))

       :else
       (let [r (rand-int 100)]
         (if (<= p2max r) p2 p1))))))


(defn end-turn [game-map]
; allow only if two player finished their turn :turn-ends-2
  (let [[p1 p2] (get-players game-map)]
    (-> game-map
        (delete-all-messages)
        (assoc-in [:turn-ends] 0)
        (take-cards p1 cards-per-turn)
        (take-cards p2 cards-per-turn)
        ;; decrement durations
        (decrement-durations)
        ;; random card
        (#(let [c (c/next-card)
                win3 (third-card-winner game-map)]
            (-> %
                (take-card win3 c)
                (log-message (str "Card " c " goes to " (get-player-name-by-id game-map win3)))
                )))
        ;; increment turn num
        (update-in [:turn] inc)
        (#(log-message % (str "Turn: " (:turn %))))
        )))



;; Functions to operate on game state

(def terminal-hierarchy
  (-> (make-hierarchy)
      (derive :terminal-left-paren :terminal)
      (derive :terminal-right-paren :terminal)
      (derive :terminal-left-square :terminal)
      (derive :terminal-right-square :terminal)

      (derive :terminal-id :terminal-param)
      (derive :terminal-num :terminal-param)

      (derive :duration-luck-1 :duration)
      (derive :duration-luck-2 :duration)
      ))

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
    (when (and (= :terminal-id card)
               (not (base/id? param)))
      (throw (IllegalArgumentException. "Not valid ID")))
    (when (and (= :terminal-num card)
               (not (base/num? param)))
      (throw (IllegalArgumentException. "Not valid NUM")))
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
  :cleanup
  [game player-key card & args]
  ((apply-to-funstruct game player-key)
   (fn [funstruct]
     (remove #(= :gap (:terminal %)) funstruct))))

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Durations
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod apply-card
  :duration
  [game-map player-key card & args]
  ((apply-to-board game-map player-key)
   (fn [v]
     (let [c (get-in c/cards [card])]
       (conj v {:key card
                :turns-left (:turns-left c)})))))

(defmethod apply-card
  :default
  [game-map player-key card & args]
  ((u/log "Unsupported card played " card)
   game-map))

;;; API


(defn- check-for-win
  "Check if some player won game,
   and sets gamemap key :winner [p1 | p2 | :draw]"
  [game]
  (let [[p1 p2] (get-players game)
        f1 (get-funstruct game p1)
        p1-goal (get-goal game p1)
        f2 (get-funstruct game p2)
        p2-goal (get-goal game p2)
        p1-done (base/completed-funstruct? (:raw p1-goal) f1)
        p2-done (base/completed-funstruct? (:raw p2-goal) f2)]
    (cond
     (= false p1-done p2-done) game
     (= true p1-done p2-done) (assoc game :win :draw)
     p1-done (assoc game :win p1)
     p2-done (assoc game :win p2)
     :else game
     )))

(defn- duration-duplicate-active? [board]
  (:duration-duplicate (into #{} (map :key board))))

(defn- process-duration-duplicate [game player card]
  (let [opp (get-opponent game player)
        board (get-board game opp)]
    (if (duration-duplicate-active? board)
      (take-card game opp card)
      game)))

;; TODO exception safe
(defn use-card
  "Player Key:  UUID of player
     Card Pos:  index of card in :cards vector
         Args:  additional args specific for function"
  [game-map player-key card-pos & args]
  (let [card (get-card game-map player-key card-pos)]
    (try
      (-> game-map
          (delete-all-messages)
          ;; TODO validate such card is present
          (#(apply apply-card % player-key card args))
          ;; delete card from player
          (delete-card player-key card-pos)
          (log-message (str (get-player-name-by-id game-map player-key) " played " card))

          ;; duration effects
          (process-duration-duplicate player-key card)
          
          (check-for-win)
          ;; log winner
          (#(let [winner (:win %)]
              (cond
               (= winner :draw) (log-message % "Draw!")
               winner (log-message % (str "Winner is " winner))
               :else %)))
          )
      (catch Exception e
        (do
          (u/log
           (.getMessage e)
           "\n"
           "Critical Error:\n\n"
           "Gamestate: " game-map "\n"
           "Player:"     player-key "\n"
           "Card"        (get-card game-map player-key card-pos) "\n"
           "Args[]:"     args "\n")
          game-map))))) ;; scary, but works



;; Examples
;; Use :terminal
;; (use-card :g :p 0 :position-in-funstruct)
;; Use :terminal-param
;; (use-card :g :p 0 :position-in-funstruct value)
;; Use :mutator-left-paren, :mutator-right:paren
;; (use-card :g :p 0)
;; Use :mutator-position-gap
;; (use-card :g :p 0 :position-after-in-funstruct)
