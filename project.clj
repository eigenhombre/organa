(defproject organa "0.0.1-SNAPSHOT"
  :url "https://github.com/eigenhombre/organa"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.12.2"]
                 [enlive "1.1.6"]
                 [environ "1.1.0"]
                 [garden "1.3.2"]
                 [mount "0.1.11"]
                 [org.clojars.zcaudate/watchtower "0.1.2"]
                 [hiccup "1.0.5"]]
  :target-path "target/%s"
  :uberjar-name "organa.jar"
  :profiles {:uberjar {:aot :all}}
  :main ^:skip-aot organa.core)
