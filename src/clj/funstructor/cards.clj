(ns funstructor.cards)

;; List of cards

(def available-cards
  #{:terminal-left-paren
    :terminal-right-paren
    :terminal-left-square
    :terminal-right-square
    :terminal-id
    :terminal-num

    :mutator-left-gap
    :mutator-right-gap
    :mutator-position-gap
    ;:mutator-shot

    :action-thief-1
    :action-discard-1
    :action-equality-1
    :action-equality-2
    :action-equality-3

    :duration-luck-1
    :duration-luck-2
    
    })

(def cards
  {

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Terminals
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

   :terminal-left-paren
   {:name "Left Paren"
    :description
    "fill the gap on your funstruct with left paren"
    :value :left-paren
    :target :self
    :type :terminal
    :weight 100
    :img "terminal_left_paren.svg"}

   :terminal-right-paren
   {:name "Right Paren"
    :description
    "fill the gap on your funstruct with right paren"
    :value :right-paren
    :target :self
    :type :terminal
    :weight 100
    :img "terminal_right_paren.svg"}

   :terminal-left-square
   {:name "LEFT SQUARE"
    :description
    "fill the gap on your funstruct with left square"
    :value :left-square
    :target :self
    :type :terminal
    :weight 100
    :img "terminal_left_square.svg"}

   :terminal-right-square
   {:name "RIGHT SQUARE"
    :description
    "fill the gap on your funstruct with right square"
    :value :right-square
    :target :self
    :type :terminal
    :weight 100
    :img "terminal_right_square.svg"}

   :terminal-id
   {:name "ID"
    :description
    "fill the gap on your funstruct with identifier, according to clojure regexp"
    :value :id
    :target :self
    :type :terminal
    :weight 100
    :img "terminal_id.svg"}

   :terminal-num
   {:name "Number"
    :description
    "fill the gap on your funstruct with integer number"
    :value :num
    :target :self
    :type :terminal
    :weight 50
    :img "terminal_num.svg"}

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Mutators
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

   :mutator-left-gap
   {:name "Left Gap"
    :description
    "add gap to the leftmost position of your funstruct"
    :target :self
    :type :mutator
    :weight 75
    :img "mutator_left_gap.svg"}

   :mutator-right-gap
   {:name "Right Gap"
    :description
    "add gap to the rightmost position of your funstruct"
    :target :self
    :type :mutator
    :weight 75
    :img "mutator_right_gap.svg"}

   :mutator-position-gap
   {:name "Gap"
    :description
    "add gap to the specific position of your funstruct"
    :target :self
    :type :mutator
    :weight 150
    :img "mutator_pos_gap.svg"}

   :mutator-shot
   {:name "Shot"
    :description
    "remove terminal from the opponents funstruct"
    :target :opponent
    :type :mutator
    :weight 50
    :img "mutator_shot.svg"}


   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Actions
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

   :action-thief-1
   {:name "Thief v1.0"
    :description
    "steal random card from opponent's hand"
    :target :self
    :type :action
    :weight 40
    :img "action_thief_1.svg"}

   :action-discard-1
   {:name "Discard v1.0"
    :description
    "opponent discards random card"
    :target :self
    :type :action
    :weight 40
    :img "action_discard_1.svg"}

   :action-equality-1
   {:name "Equality v1"
    :description
    "you and your opponent discard all cards from hand
and take 5 from deck"
    :target :self
    :type :action
    :weight 20
    :img "action_equality_1.svg"}

   :action-equality-2
   {:name "Equality v2"
    :description
    "you take cards from deck until you have as many as your opponent"
    :target :self
    :type :action
    :weight 20
    :img "action_equality_2.svg"}

   :action-equality-3
   {:name "Equality v3"
    :description
    "you and your opponent discard all cards"
    :target :self
    :type :action
    :weight 10
    :img "action_equality_3.svg"}

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Duration
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

   :duration-luck-1
   {:name "Luck v1.0"
    :description
    "increases your chances for getting third card to 75%. Lasts 3 turns"
    :target :self
    :type :duration
    :weight 10
    :turns-left 3
    :img "duration_luck_1.svg"}

   :duration-luck-2
   {:name "Luck v2.0"
    :description
    "increases your chances for getting third card to 100%. Lasts 3 turns"
    :target :self
    :type :duration
    :weight 10
    :turns-left 3
    :img "duration_luck_2.svg"}


   })

(defn- in? [n a b]
  (and (<= a n) (<  n b)))

(defn- weights []
  (->> (select-keys cards available-cards)
       (map (fn [[k v]] (let [w (:weight v)] [k w])))
       (reduce (fn [vec [key weight]]
                 (let [[_ [_ b]] (nth vec (dec (count vec)))]
                   (conj vec [key [b (+ b weight)]]))
                 ) [[:guard [0 0]]])
       (#(subvec % 1))))


(defn next-card []
  (let [ws (weights)
        [_ [_ total]] (last ws)
        random (rand-int total)]
    (ffirst
     (filter (fn [[k [a b]]] (in? random a b)) ws))))
