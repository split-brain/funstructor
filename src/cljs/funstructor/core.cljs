(ns funstructor.core
  (:require [chord.client :refer [ws-ch]]
            [cljs.core.async :refer [chan <! >! put! close! timeout]]
            [reagent.core :as reagent :refer [atom]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

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

(reagent/render-component [home] (.getElementById js/document "app"))
