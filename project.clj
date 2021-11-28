(defproject organa "0.0.1-SNAPSHOT"
  :description "An org-mode-based blogging engine"
  :url "https://github.com/eigenhombre/organa"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [cheshire "5.10.1"]
                 [clj-rss "0.3.0"]
                 [clj-time "0.15.2"]
                 [clj-yaml "0.4.0"]
                 [clojure.java-time "0.3.3"]
                 [enlive "1.1.6"]
                 [environ "1.2.0"]
                 [garden "1.3.10"]
                 [hiccup "1.0.5"]
                 [me.raynes/fs "1.4.6"]
                 [mount "0.1.16"]
                 [net.mikera/imagez "0.12.0"]]
  :target-path "target/%s"
  :uberjar-name "organa.jar"
  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]}
  :profiles {:uberjar {:aot :all}
             :kaocha {:dependencies [[lambdaisland/kaocha "1.60.945"]]}
             :dev {:plugins [[lein-bikeshed "0.5.2"]
                             [lein-ancient "1.0.0-RC3"]
                             [lein-codox "0.10.8"]
                             [jonase/eastwood "0.9.9"]
                             [lein-kibit "0.1.8"]]}}
  :codox {:output-path "docs"
          :source-uri
          {#".*"
           "https://github.com/eigenhombre/organa/blob/master/{filepath}#L{line}"}}
  :main ^:skip-aot organa.core)
