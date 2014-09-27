(ns funstructor.cards)

;; List of cards

(def cards
  {

   :terminal-left-paren
   {:name "LEFT PAREN"
    :description
    "Fill the gap with left parenthesis"
    :value :left-paren
    :target :self
    :type :terminal}

   :terminal-right-paren
   {:name "RIGHT PAREN"
    :description
    "Fill the gap with right parenthesis"
    :value :right-paren
    :target :self
    :type :terminal}

   :terminal-left-square
   {:name "LEFT SQUARE"
    :description
    "Fill the gap with left square bracket"
    :value :left-square
    :target :self
    :type :terminal}

   :terminal-right-square
   {:name "RIGHT SQUARE"
    :description
    "Fill the gap with right square bracket"
    :value :right-square
    :target :self
    :type :terminal}

   :terminal-id
   {:name "ID"
    :description
    "Fill the gap with identifier, enter the name"
    :value :id
    :target :self
    :type :terminal}

   :terminal-num
   {:name "NUM"
    :description
    "Fill the gap with number, enter the number"
    :value :num
    :target :self
    :type :terminal}

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Mutators
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

   :mutator-left-gap
   {:name "Left Gap"
    :description
    "Add gap to the leftmost position"
    :target :self
    :type :mutator}

   :mutator-right-gap
   {:name "Right Gap"
    :description
    "Add gap to the rightmost position"
    :target :self
    :type :mutator}

   :mutator-position-gap
   {:name "Position Gap"
    :description
    "Add gap to the specific position, select position"
    :target :self
    :type :mutator}
   
   
   })

(defn next-card []
  (rand-nth (keys cards)))
