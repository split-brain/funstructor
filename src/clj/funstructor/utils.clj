(ns funstructor.utils
  (:require [clojure.core.async :refer [chan >! <! go go-loop timeout]]))

(defn printerr [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn gen-uuid []
  (java.util.UUID/randomUUID))

(defn uuid-from-string [uuid-str]
  (java.util.UUID/fromString uuid-str))

(defn delete-from-vector [vector pos]
  (vec (concat (take pos vector)
               (drop (inc pos) vector))))

(def log-chan (chan))

(defn start-logging []
  (go-loop []
    (apply println (concat (<! log-chan) ["\n"]))
    (recur)))

(defn log [& args]
  (go
    (>! log-chan args)))

(defn debug [e]
  (println e)
  e)
