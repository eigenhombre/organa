(set-env! :dependencies '[[clj-time "0.12.2"]
                          [enlive "1.1.6"]
                          [environ "1.1.0"]
                          [garden "1.3.2"]
                          [mount "0.1.11"]
                          [org.clojars.zcaudate/watchtower "0.1.2"]
                          [hiccup "1.0.5"]]
          :source-paths #{"src"}
          :resource-paths #{"src"})


(task-options! aot {:all true}
               pom {:project 'organa
                    :version "0.1.0"}
               jar {:file "organa.jar"
                    :main 'organa.core}
               target {:dir #{"target/"}})


(deftask build []
  (comp (aot) (pom) (uber) (jar) (target)))
