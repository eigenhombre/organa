(ns organa.config
  "
  Generate a map of configuration values.

  This should eventually be more configurable.
  "
  (:require [environ.core :as env]))

(def config
  (let [home-dir (env/env :home)
        remote-host "johnj.com"]
    {:home-dir home-dir
     :remote-host remote-host
     :site-source-dir (str home-dir "/org/sites/" remote-host)
     :target-dir "/tmp/organa"}))
