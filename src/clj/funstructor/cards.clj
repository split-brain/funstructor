(ns funstructor.cards)

;; List of cards

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
    :img "terminal_left_paren.svg"}

   :terminal-right-paren
   {:name "Right Paren"
    :description
    "fill the gap on your funstruct with right paren"
    :value :right-paren
    :target :self
    :type :terminal
    :img "terminal_right_paren.svg"}

   :terminal-left-square
   {:name "LEFT SQUARE"
    :description
    "fill the gap on your funstruct with left square"
    :value :left-square
    :target :self
    :type :terminal
    :img "terminal_left_square.svg"}

   :terminal-right-square
   {:name "RIGHT SQUARE"
    :description
    "fill the gap on your funstruct with right square"
    :value :right-square
    :target :self
    :type :terminal
    :img "terminal_right_square.svg"}

   :terminal-id
   {:name "ID"
    :description
    "fill the gap on your funstruct with identifier, according to clojure regexp"
    :value :id
    :target :self
    :type :terminal
    :img "terminal_id.svg"}

   :terminal-num
   {:name "Number"
    :description
    "fill the gap on your funstruct with integer number"
    :value :num
    :target :self
    :type :terminal
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
    :img "mutator_left_gap.svg"}

   :mutator-right-gap
   {:name "Right Gap"
    :description
    "add gap to the rightmost position of your funstruct"
    :target :self
    :type :mutator
    :img "mutator_right_gap.svg"}

   :mutator-position-gap
   {:name "Gap"
    :description
    "add gap to the specific position of your funstruct"
    :target :self
    :type :mutator
    :img "mutator_pos_gap.svg"}
   
   
   })

(defn next-card []
  (rand-nth (keys cards)))
