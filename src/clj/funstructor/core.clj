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
            [taoensso.timbre :as t]

            [funstructor.processes :as p]
            [funstructor.utils :as u])
  (:gen-class))

(def channels-to-close (atom #{}))

(defn register-channel [ch]
  (swap! channels-to-close #(conj % ch)))

(defn close-registered-channels []
  (doseq [ch @channels-to-close]
    (a/close! ch))
  (reset! channels-to-close #{}))







(def pending-players-chan (atom nil))

(defn ws-handler [{:keys [ws-channel] :as req}]
  (t/info "Opened connection from" (:remote-addr req))
  (register-channel ws-channel)
  (p/handshake-process ws-channel @pending-players-chan))

(defroutes app-routes
  (GET "/cljs" [] (redirect "index-cljs.html"))
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/ws" [] (-> ws-handler
                    (wrap-websocket-handler {:format :str
                                             :read-ch (a/chan nil nil u/print-exception-stacktrace)
                                             :write-ch (a/chan nil nil u/print-exception-stacktrace)})))
  (route/resources "/")
  (route/not-found "Page not found"))

(def handler
  (handler/site app-routes))

(def application (-> handler
                     (reload/wrap-reload)))





(def stop-server-fn (atom nil))

(defn start-server []
  (if @stop-server-fn
    (throw (RuntimeException. "Server is already running"))
    (let [port (Integer/parseInt
                (or (System/getenv "PORT") "8080"))]
      (t/info "Server started on port " port)
      (reset! pending-players-chan (a/chan))
      (register-channel @pending-players-chan)
      (p/pending-checker-process @pending-players-chan)
      (let [stop-fn (run-server application {:port port :join? false})]
        (reset! stop-server-fn stop-fn)))))

(defn stop-server []
  (if-let [stop-fn @stop-server-fn]
    (do (stop-fn)
        (close-registered-channels)
        (reset! stop-server-fn nil))
    (throw (RuntimeException. "Server has not been started"))))

(defn -main [& args]
  (start-server))
