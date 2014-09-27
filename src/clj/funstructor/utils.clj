(ns funstructor.utils)

(defn printerr [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn gen-uuid []
  (java.util.UUID/randomUUID))
