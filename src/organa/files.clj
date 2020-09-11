(ns organa.files
  (:require [clojure.java.io :as io]))

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
         (remove (comp #(.contains % "/.") str)))))

(defn basename [f]
  (let [fil (if (string? f) (io/file f) f)]
    (.getName fil)))
