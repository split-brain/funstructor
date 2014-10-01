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

            [funstructor.commands :as commands]
            [funstructor.utils :as u])
  (:gen-class))

(defn ws-handler [{:keys [ws-channel] :as req}]
  (u/log "Opened connection from" (:remote-addr req))
  (a/go-loop []
    (let [{:keys [message error] :as msg} (a/<! ws-channel)]
      (u/log "Received message" msg)


      (if (nil? msg)
        (commands/handle-command (commands/disconnected-command) ws-channel)
        (do
          (if error
            (u/printerr "Received error: " msg)
            (commands/handle-command (commands/decode-command message) ws-channel))
          (recur))))))

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
    (commands/pending-checker)
    (run-server application {:port port :join? false})))
