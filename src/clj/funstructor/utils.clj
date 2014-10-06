(ns funstructor.utils
  (:require [clojure.core.async :refer [chan >! <! go go-loop timeout]]))

(def id-counter (atom 0))

(defn next-id! []
  (swap! id-counter inc))

(defn printerr [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn delete-from-vector [vector pos]
  (vec (concat (take pos vector)
               (drop (inc pos) vector))))

(defn print-exception-stacktrace [e]
  (.printStackTrace e))

;; Logging routines

(def log-chan (chan))

(defn logging-process []
  (go-loop []
    (apply println (concat (<! log-chan) ["\n"]))
    (recur)))

(defn log [& args]
  (go
    (>! log-chan args)))

(defn debug [str e]
  (log str e)
  e)
