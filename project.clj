(defproject organa "0.0.1-SNAPSHOT"
  :url "https://github.com/eigenhombre/organa"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cheshire "5.8.1"]
                 [clj-rss "0.2.5"]
                 [clj-time "0.12.2"]
                 [clojure.java-time "0.3.2"]
                 [enlive "1.1.6"]
                 [environ "1.1.0"]
                 [garden "1.3.2"]
                 [hiccup "1.0.5"]
                 [marginalia "0.9.1"]
                 [mount "0.1.11"]
                 [net.mikera/imagez "0.12.0"]]
  :target-path "target/%s"
  :uberjar-name "organa.jar"
  :profiles {:uberjar {:aot :all}}
  :main ^:skip-aot organa.core)
