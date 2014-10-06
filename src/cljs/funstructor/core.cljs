(ns funstructor.core
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [chan <! >! put! close! timeout]]
            [reagent.core :as r]
            [funstructor.view :as v])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn process-msg [msg ch]
  (js/alert (str "Message recieved: " (prn-str msg))))

(defn start-new-game! [player-name]
  (go
    (let [ws-url "ws://localhost:8080/ws"
          {:keys [ws-channel error]} (<! (ws-ch ws-url {:format :json-kw}))]
      (if error
        (js/alert "Failed to open WebSocket connection")
        (do
          (>! ws-channel {:type "game-request" :data {:user-name player-name}})
          (go-loop [msg (<! ws-channel)]
            (process-msg msg ws-channel)
            (recur (<! ws-channel))
            ))))))

(defn ^:export run []
  (r/render-component [v/game start-new-game!] (v/get-element "app")))
