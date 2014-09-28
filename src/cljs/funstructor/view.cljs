(ns funstructor.view
  (:require [reagent.core :as reagent :refer [atom]]))

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

(defn render [component]
  (reagent/render-component [component] (.getElementById js/document "app")))
