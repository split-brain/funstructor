(ns funstructor.core
  (:use ring.util.response
        org.httpkit.server)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.reload :as reload]
            [ring.middleware.stacktrace :as st]
            [chord.http-kit :refer [wrap-websocket-handler]]
            [clojure.core.async :as a]
            [compojure.core :refer [defroutes GET]]

            [funstructor.processes :as p]
            [funstructor.utils :as u])
  (:gen-class))

(defn ws-handler [{:keys [ws-channel] :as req}]
  (u/log "Opened connection from" (:remote-addr req))
  (p/handshake-process ws-channel p/pending-players-chan))

(defroutes app-routes
  (GET "/cljs" [] (redirect "index-cljs.html"))
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/ws" [] (-> ws-handler
                    (wrap-websocket-handler {:format :str
                                             :read-ch (a/chan nil nil #(.printStackTrace %))
                                             :write-ch (a/chan nil nil #(.printStackTrace %))})))
  (route/resources "/")
  (route/not-found "Page not found"))

(def handler
  (handler/site app-routes))

(def application (-> handler
                     ;(st/wrap-stacktrace)
                     (reload/wrap-reload)
                     ))

(defn -main [& args]
  (let [port (Integer/parseInt
              (or (System/getenv "PORT") "8080"))]
    (u/logging-process)
    (u/log "Server started on port " port)
    (p/pending-checker-process p/pending-players-chan)
    (run-server application {:port port :join? false})))
