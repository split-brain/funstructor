(ns funstructor.cards-functions
  (:require
   [funstructor.utils :as u]))

;; Functions to operate on game state

(declare apply-card)

;; TODO card accessor

(defn apply-to-cards
  "Get access to clojure"
  [game-map player-key]
  (partial update-in game-map [player-key :cards]))

(defn take-card
  "Add card to player state"
  [game-map player-key card]
  ((apply-to-cards game-map player-key)
   (fn [v] (conj v card))))

(defn delete-card
  "Delete card from player state"
  [game-map player-key card-pos]
  ((apply-to-cards game-map player-key)
   (fn [v] (u/delete-from-vector v card-pos))))

(defn use-card
  "Player Key:  UUID of player
     Card Pos:  index of card in :cards vector
         Args:  additional args specific for function"
  [game-map player-key card-pos & args]
  (-> game-map
      
      ;; apply card
      
      ;; delete card from player
      
      ))
