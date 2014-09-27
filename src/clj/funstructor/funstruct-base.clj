(ns funstructor.funstruct-base)


(def funstructs
  {:abs
   "(defn abs [x] (if (pos? x) x (- x)))"

   :even?
   ""})

(def id-regex "[a-zA-Z\\?\\!\\+\\-\\*\\/][a-zA-Z\\?\\!\\+\\-\\*\\/0-9]*")

(defn id? [s]
  (re-matches (re-pattern id-regex) s))

(defn num? [s]
  (try (Long/parseLong s)
       (catch NumberFormatException e nil)))

(defn token->terminal [token]
  (let [m {"(" {:terminal :left-paren}
           ")" {:terminal :right-paren}
           "[" {:terminal :left-square}
           "]" {:terminal :right-square}}
        detected (get m token)]
    (cond
     detected detected
     (id? token) {:terminal :id :value token}
     (num? token) {:terminal :num :value token}
     :else (throw (IllegalArgumentException.
                   "Invalid token")))))

(defn s->funstruct [s]
  (-> (str "\\(|\\)|\\[|\\]|[0-9]+|" id-regex)
      (re-pattern)
      (re-seq s)
      (#(map token->terminal %))
      (vec)))
