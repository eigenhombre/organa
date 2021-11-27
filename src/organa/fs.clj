(ns organa.fs
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [me.raynes.fs :as fs])
  (:import [java.io File]))

(defmacro with-tmp-dir [dir-file & body]
  `(let [~dir-file (fs/temp-dir "organa")]
     (try
       ~@body
       (finally
         ;; FIXME: Eliminate double-evaluation:
         (fs/delete-dir ~dir-file)))))

(defn path [& args]
  (string/join "/" args))

(defn dirfile [^File f]
  (io/file (.getParent f)))
