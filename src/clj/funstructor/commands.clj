(ns funstructor.commands
  (:require [clojure.core.async :as a]
            [cheshire.core :as chs]

            [funstructor.utils :as u]))

(def server-side-commands
  #{:game-request
    :start-game-ok
    :end-turn
    :action
    :chat-message})

;; TODO: Create make function for every command

(defn encode-cmd [command]
  (chs/generate-string command))

(defn decode-cmd [in-msg]
  (let [{:keys [message error]} in-msg]
    (and in-msg
         (if error
           {:type :ws-error
            :data error}
           (try
             (update-in
              (chs/parse-string message true)
              [:type]
              keyword)
             (catch java.io.IOException e {:type :decode-error :data message}))))))

(defn branch-channel [ws-channel]
  ;; TODO: Figure out how to transform channel values
  ;; Solution: use transduser
  (let [pub-ch (a/pub ws-channel #(:type (decode-cmd %)))]
    {:write-ch ws-channel
     :branches
     (into {}
           (map
            #(do
               (a/sub pub-ch %1 %2)
               [%1 %2])
            server-side-commands
            (repeatedly #(a/chan nil nil u/print-exception-stacktrace))))}))

(defn get-branch-ch [br-ch cmd-type]
  (get-in br-ch [:branches cmd-type]))

(defn get-branch-chs [br-ch & cmd-types]
  (mapv #(get-branch-ch br-ch %) cmd-types))

(defn find-br-ch-for-cmd-ch [cmd-ch br-chs]
  (some
   (fn [br-ch]
     (let [all-cmd-chs (set (vals (:branches br-ch)))]
       (when (all-cmd-chs cmd-ch)
         br-ch)))
   br-chs))

(defmacro read-cmd-from-chs [br-chs cmd-types & {:keys [timeout-ch]}]
  `(let [br-chs# ~br-chs
         cmd-chs# (vec (mapcat #(apply get-branch-chs % ~cmd-types) br-chs#))
         [value# ch# :as tuple#] (a/alts! (if ~timeout-ch
                                            (conj cmd-chs# ~timeout-ch)
                                            cmd-chs#))]
     (if (and ~timeout-ch (= ch# ~timeout-ch))
       tuple#
       [(if (nil? value#) value# (decode-cmd value#))
        (find-br-ch-for-cmd-ch ch# br-chs#)])))


(defmacro read-cmd-by-type [br-ch cmd-type]
  `(first
    (read-cmd-from-chs [~br-ch]
                       [~cmd-type])))

(defmacro write-cmd-to-ch [br-ch cmd]
  `(a/>! (:write-ch ~br-ch) (encode-cmd ~cmd)))

(defmacro write-cmd-to-chs [br-chs cmd & async-write]
  `(let [cmd# ~cmd
         async-write# ~@async-write]
     (doseq [br-ch# ~br-chs]
       (if async-write#
         (a/go (write-cmd-to-ch br-ch# cmd#))
         (write-cmd-to-ch br-ch# cmd#)))))
