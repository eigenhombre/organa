(ns organa.io
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(defn dirfile [f]
  (io/file (.getParent f)))

(defn path [& args]
  (string/join "/" args))
