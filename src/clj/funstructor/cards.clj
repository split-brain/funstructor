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
    :mutator-position-gap})

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
    :weight 100
    :img "mutator_left_gap.svg"}

   :mutator-right-gap
   {:name "Right Gap"
    :description
    "add gap to the rightmost position of your funstruct"
    :target :self
    :type :mutator
    :weight 100
    :img "mutator_right_gap.svg"}

   :mutator-position-gap
   {:name "Gap"
    :description
    "add gap to the specific position of your funstruct"
    :target :self
    :type :mutator
    :weight 100
    :img "mutator_pos_gap.svg"}
   
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
