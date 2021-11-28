(ns organa.fs
  "
  Functions for interacting with files, paths, and directories in the
  file system.
  "
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [me.raynes.fs :as fs])
  (:import [java.io File]))

(defmacro with-tmp-dir
  "
  Create temporary file, bind to `dir-file`, and execute `body` in
  that context, removing the directory afterwards.
  "
  [dir-file & body]
  `(let [~dir-file (fs/temp-dir "organa")]
     (try
       ~@body
       (finally
         ;; FIXME: Eliminate double-evaluation:
         (fs/delete-dir ~dir-file)))))

(defn path
  "
  Create a file or directory path (string) out of `args`.

      (path \"/a\" \"b\" \"c.txt\")
      ;;=>
      \"a/b/c.txt\"
  "
  [& args]
  (string/join "/" args))

(defn dirfile
  "
  Find the directory File that contains File `f`, which must exist.
  "
  [^File f]
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

(defn ^:private ^File coerce-file
  "
  Make sure `f` is a `File`.  FIXME: use protocols for this instead.
  "
  [f]
  (if (string? f)
    (io/file f)
    f))

(defn basename
  "
  Get file (not directory) portion of name of `f`, where `f` is a
  `File` or path string.
  "
  [f]
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
