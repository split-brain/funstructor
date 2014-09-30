(ns funstructor.cards-functions-test
  (:require [clojure.test :refer :all]
            [funstructor.cards-functions :refer :all]))

(deftest apply-unrecognized-card
  (testing "Should return unmodified game state when unrecognized card was applied to it."
    (let [g (make-game :p1 :p2)
          g' (apply-card g :p1 :missing-card)]
      (is (= g g')))))
