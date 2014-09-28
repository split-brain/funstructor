(ns funstructor.funstruct-base)


(def funstructs
  {
   :test-sum
   "(= (+ 2 2) 5)"

   :identity
   "(defn [x] x)"

   :abs
   "(defn abs [x] (if (pos? x) x (- x)))"

   :even?
   "(defn even? [x] (zero? (mod x 2)))"

   :odd?
   "(defn odd? [x] (= 1 (mod x 2)))"

   :sum-of-squares
   "(defn sum-of-squares [coll] (reduce + (map square coll)))"

   :factorial
   "(defn factorial [num] (reduce * (range 1 (inc num))))"

   })

(def id-regex "[a-zA-Z\\?\\!\\+\\-\\*\\/\\=][a-zA-Z\\?\\!\\+\\-\\*\\/0-9\\=]*")

(defn id? [s]
  (re-matches (re-pattern id-regex) s))

(defn num? [s]
  (try (Long/parseLong s)
       (catch NumberFormatException e nil)))

(defn- token->terminal [token]
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

(defn- s->funstruct [s]
  (-> (str "\\(|\\)|\\[|\\]|[0-9]+|" id-regex)
      (re-pattern)
      (re-seq s)
      (#(map token->terminal %))
      (vec)))

(defn get-two-random-funstructs []
  (let [[f1 f2] (take 2 (shuffle (keys funstructs)))
        s1 (get funstructs f1) s2 (get funstructs f2)]
    [{:name f1 :raw s1}
     {:name f2 :raw s2}]))

(defn completed-funstruct?
  [raw current-funstruct]
  (let [need-funstruct (s->funstruct raw)]
    (->> current-funstruct
         (map (fn [e]
                (if-not (:value e) (dissoc e :value) e)))
         (remove (fn [e] (= :gap (:terminal e))))
         (= need-funstruct))))

(defn print-terminal-count []
  (doseq [k (keys funstructs)]
    (let [raw (get funstructs k)
          funs (s->funstruct raw)]
      (println k ":" (count funs)))))
