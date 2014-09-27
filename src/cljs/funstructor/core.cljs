(ns funstructor.core
  (:require
            ;[chord.client :refer [ws-ch]]
            ;[cljs.core.async :refer [chan <! >! put! close! timeout]]
            [reagent.core :as reagent :refer [atom]])
  ;(:require-macros [cljs.core.async.macros :refer [go go-loop]])
  )



;; (defn opponend-field []
;;   [:div.opponent-field
;;    [:h1 "TEST!!!"]])

;; (defn field []
;;   [:div.field])

;; (defn hand []
;;   [:div.hand])

;; (defn legend []
;;   [:div.legend])

;; (defn game []
;;   [:div
;;    [:h1 "FUNSTRUCTOR!"]])

(defn child [name]
  [:p "Hi, I am " name])

(defn childcaller []
  [child "Foo Bar"])

(reagent/render-component [childcaller] (.getElementById js/document "app"))

;; (defn client-ws-handler []
;;   (go
;;     (let [{:keys [ws-channel error]} (<! (ws-ch "ws://localhost:8080/ws"))]
;;       (if error
;;         (js/alert (pr-str error))
;;         (loop []
;;           (<! (timeout 1000))
;;           ;; (js/alert "writing message")
;;           (>! ws-channel "Hello world!")
;;           (recur))))))

;; (set! (.-onload js/window)
;;       client-ws-handler)
