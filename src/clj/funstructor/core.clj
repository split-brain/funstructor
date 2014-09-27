(ns funstructor.core
  (:use ring.util.response
        org.httpkit.server)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.reload :as reload]
            [chord.http-kit :refer [wrap-websocket-handler]]
            [clojure.core.async :refer [<! >! put! close! go-loop]]
            [compojure.core :refer [defroutes GET]]))

(defn ws-handler [{:keys [ws-channel] :as req}]
  (println "Opened connection from" (:remote-addr req))
  (go-loop []
    (when-let [{:keys [message error] :as msg} (<! ws-channel)]

      (recur))))

(defroutes app-routes
  (GET "/" [] (redirect "index.html"))
  (GET "/ws" [] (-> ws-handler
                    (wrap-websocket-handler {:format :json-kw})))
  (route/resources "/")
  (route/not-found "Page not found"))

(def handler
  (handler/site app-routes))

(def application (-> handler
                     reload/wrap-reload))

(defn -main [& args]
  (let [port (Integer/parseInt
               (or (System/getenv "PORT") "8080"))]
    (println "Server started on port " port)
    (run-server application {:port port :join? false})))

