(ns funstructor.core
  (:require
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [chan <! >! put! close! timeout]]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn init-ws-connection! []
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:8080/ws"
                                                {:format :json-kw}))]
      (if-not error
        ws-channel
        (js/alert "Error:" (pr-str error))))))

;; (defn client-ws-handler []
;;   (go
;;     (let [{:keys [ws-channel error]} (<! ws)]
;;       (if error
;;         (js/alert (pr-str error))
;;         (loop []
;;           (<! (timeout 1000))
;;           (recur))))))

;; (set! (.-onload js/window)
;;       client-ws-handler)

(defn opponent-field []
  [:div.opponent-field])

(defn field []
  [:div.field])

(defn hand []
  [:div.hand])

(defn legend []
  [:div.legend])

(defn process-msg [msg ch]
  (js/alert (str "Message recieved: " (prn-str msg))))

(def url "ws://localhost:8080/ws")

(defn start-new-game! [player-name]
  (render-loader)
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch url {:format :json-kw}))]
      (if error
        (js/alert "Failed to open WebSocket connection")
        (do
          (>! ws-channel {:type "game-request" :data {:user-name player-name}})
          (go-loop [msg (<! ws-channel)]
            (process-msg msg ws-channel)
            (recur (<! ws-channel))
            ))))))

(defn login-form []
  [:section#start
   [:form#startForm {:action "#"}
    [:div.input.start
     [:label "Your name"]
     [:input {:type "text"}]]
    [:div.cb.h20]
    [:div.button.start
     [:input {:value "Play"
              :type "submit"
              :on-click start-new-game!}]]]])

(defn game []
  [:section#game
   [opponent-field]
   [field]
   [hand]
   [legend]]
  [:div#flyingCards]
  [login-form])

(reagent/render-component [game] (.getElementById js/document "app"))
