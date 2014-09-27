(ns funstructor.brepl
  (:require
   [cemerick.piggieback]
   [cljs.repl.browser]))

(defn brepl []
  (cemerick.piggieback/cljs-repl
   :repl-env (cljs.repl.browser/repl-env :port 9000)))
