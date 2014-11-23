(defproject funstructor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies[[org.clojure/clojure "1.7.0-alpha1"]
                [org.clojure/clojurescript "0.0-2356"]
                [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                [http-kit "2.1.16"]
                [compojure "1.1.9"]
                [jarohen/chord "0.4.2" :exclusions [org.clojure/clojure]]
                [ring "1.3.1"]
                [hiccup "1.0.5"]
                [cheshire "5.3.1"]
                [reagent "0.4.2"]
                [com.taoensso/timbre "3.3.1"]
                [javax.servlet/servlet-api "2.5"]
                [com.cemerick/piggieback "0.1.3"]]

  :uberjar-name "funstructor-standalone.jar"
  :min-lein-version "2.0.0"
  :plugins [[lein-cljsbuild "1.0.3"]]
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :source-paths ["src/clj" "src/cljs"]
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :compiler
                        {:preamble ["reagent/react.js"]
                         :output-to "resources/public/js/app.js"
                         :source-map "resources/public/js/app.js.map"
                         ;:output-dir "resources/public/js"
                         :optimizations :whitespace
                         :preety-print true
                         }}

                       {:id "release"
                        :source-paths ["src/cljs"]
                        :compiler
                        {:output-to "resources/public/js/app.min.js"
                         :output-dir "resources/public/js"
                         :source-map "resources/public/js/app.js.map"
                         :optimizations :advanced
                         :pretty-print false
                         :output-wrapper false
                         :closure-warnings {:non-standard-jsdoc :off}}}]}

  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[com.cemerick/austin "0.1.5"]]}
             :production {:env {:production true}}}
  :main funstructor.core)
