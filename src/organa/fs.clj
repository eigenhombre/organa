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

(defn files-in-directory
  "
  Get list of (non-hidden) files in directory. Examples:

    (files-in-directory \"/tmp\")
    (files-in-directory \"/tmp\" :as :str)
    (files-in-directory \"/tmp\" :as :file)
  "
  [pathstr & {:keys [as]
              :or {as :file}}]
  (let [xform (get {:str str, :file identity} as identity)]
    (->> pathstr
         io/file
         .listFiles
         (map xform)
         (remove (comp #(.contains ^String % "/.") str)))))

(defn ^:private ^File coerce-file [f]
  (if (string? f)
    (io/file f)
    f))

(defn basename [f]
  (.getName (coerce-file f)))

(defn splitext
  "
  Split non-extension portion of file name from extension.

    (splitext \"a\") ;;=> '(\"a\" nil)
    (splitext \"a.b\") ;;=> '(\"a\" \"b\")
    (splitext \"a.b.c\") ;;=> '(\"a.b\" \"c\")
  "
  [f]
  (->> f
       coerce-file
       basename
       (re-find #"^(.*?)(?:\.([^\.]*))?$")
       (drop 1)))
