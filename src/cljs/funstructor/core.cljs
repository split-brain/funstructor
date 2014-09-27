(ns funstructor.core
  (:require
            [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [chan <! >! put! close! timeout]]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn opponend-field []
  [:div.opponent-field
   [:h1 "TEST!!!"]])

(defn field []
  [:div.field])

(defn hand []
  [:div.hand])

(defn legend []
  [:div.legend])

(defn game []
  [:section#game
   [opponent-field]
   [field]
   [hand]
   [legend]]
  [:div#flyingCards]
  [:section#start
   [:form#startForm {:action "#"}
    [:div.input.start
     [:label "Your name"]
     [:input {:type "text"}]]
    [:div.cb.h20]
    [:div.button.start
     [:input {:value "Play", :type "submit"}]]]])

(reagent/render-component [game] (.getElementById js/document "app"))

(defn client-ws-handler []
  (go
    (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:8080/ws"))]
      (if error
        (js/alert (pr-str error))
        (loop []
          (<! (timeout 1000))
          (>! ws-channel "Hello world!")
          (recur))))))

(set! (.-onload js/window)
      client-ws-handler)
