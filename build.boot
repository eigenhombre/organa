(set-env! :dependencies '[[environ "1.1.0"]
                          [enlive "1.1.6"]
                          [garden "1.3.2"]]
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
