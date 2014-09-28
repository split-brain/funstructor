(ns funstructor.view
  (:require [reagent.core :as reagent :refer [atom]]))

(defn ^:export get-element [id]
  (.getElementById js/document id))

(defn render [component]
  (reagent/render-component [component] (get-element "app")))

(defn- opponent-field []
  [:div.opponent-field])

(defn- field []
  [:div.field])

(defn- hand []
  [:div.hand])

(defn- legend []
  [:div.legend])

(defn loader []
  [:div#loading
   [:span.loader
    [:span.loader-inner]]])

(defn login-form [handler]

  (let [on-click-fn #(let [player-name (.-value (get-element "name"))]
                       (render loader)
                       (handler player-name))]
    [:section#start
     [:form#startForm {:action "#"}
      [:div.input.start
       [:label "Your name"]
       [:input {:type "text" :id "name"}]]
      [:div.cb.h20]
      [:div.button.start
       [:input {:value "Play"
                :type "submit"
                :on-click on-click-fn}]]]]))

(defn game [handler]
  [:div#flyingCards]
  [login-form handler])
