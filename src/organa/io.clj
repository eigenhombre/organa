(ns organa.io
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.io File]))

(defn dirfile [^File f]
  (io/file (.getParent f)))

(defn path [& args]
  (string/join "/" args))
