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

(defn exception-stacktrace-printer [e]
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

(defn debug [e]
  (log e)
  e)
