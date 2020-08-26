(ns organa.config
  "
  Generate map of configuration values.
  "
  (:require [environ.core :refer [env]]))

(def config
  (let [home-dir (env :home)
        remote-host "zerolib.com"]
    {:home-dir home-dir
     :remote-host remote-host
     :site-source-dir (str home-dir "/org/sites/" remote-host)
     :target-dir "/tmp/organa"}))

(comment
  (require '[marginalia.core :as marg])
  (marg/run-marginalia ["src/organa/config.clj"])
  (clojure.java.shell/sh "open" "docs/uberdoc.html"))
