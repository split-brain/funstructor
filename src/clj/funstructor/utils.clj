(ns funstructor.utils)

(defn printerr [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn gen-uuid []
  (java.util.UUID/randomUUID))

(defn delete-from-vector [vector pos]
  (vec (concat (take pos vector)
               (drop (inc pos) vector))))
