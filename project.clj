(defproject funstructor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies[[org.clojure/clojure "1.6.0"]
                [org.clojure/clojurescript "0.0-2342"]
                [http-kit "2.1.16"]
                [compojure "1.1.9"]
                [jarohen/chord "0.4.2"]
                [ring "1.3.1"]
                [hiccup "1.0.5"]
                [cheshire "5.3.1"]
                
                [javax.servlet/servlet-api "2.5"]

                [com.cemerick/piggieback "0.1.3"]]
  :plugins [[lein-cljsbuild "1.0.3"]]

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :source-paths ["src/clj" "src/cljs"]
  :cljsbuild {
              :builds [{:id "funstructor"
                        :source-paths ["src/cljs"]
                        :compiler {
                                   :output-to "resources/public/js/generated.js"
                                   :optimizations :whitespace
                                   :preety-print true
                                   }}]}

  :main funstructor.core)
