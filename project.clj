(defproject reagent-tutorial "0.1.0-SNAPSHOT"
  :description "A short reagent tutorial"
  :url "https://github.com/jonase/reagent-tutorial"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2138"]
                 [reagent "0.2.0"]]
  :plugins [[lein-cljsbuild "1.0.1"]]
  :cljsbuild {:builds [{:source-paths ["src-cljs"]
                        :compiler {:output-to "app.js"
                                   :optimizations :whitespace
                                   :preamble ["reagent/react.js"]
                                   :pretty-print true}}]})
