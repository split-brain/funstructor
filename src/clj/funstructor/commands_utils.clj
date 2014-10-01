(ns funstructor.command-utils
  (:require [clojure.core.async :as a]))

(defn encode-command [command]
  (chs/generate-string command))

(defn decode-command [in-str]
  (try
    (chs/parse-string in-str true)
    (catch java.io.IOException e {:type :decode-error :data in-str})))


(defn wait-for-cmd [command-type channel]
  (a/go-loop [command (decode-command (a/<! channel))]
    (if (= (:type command) command-type)
      command
      (recur (a/<! channel)))))

(defn send-cmd [command channel]
  (a/go
    (a/>! (encode command) channel)))
