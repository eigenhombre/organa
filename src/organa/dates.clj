(ns organa.dates
  (:require [net.cgrand.enlive-html :as html]
            [clj-time.coerce :refer [from-long to-long]]
            [clj-time.format :as tformat])
  (:import [java.nio.file Files]
           [java.nio.file.attribute PosixFileAttributeView]))


(def article-date-format (tformat/formatter "EEEE, MMMM d, yyyy"))


(defn date-for-org-file-by-header [path]
  (->> path
       slurp
       html/html-snippet
       (#(html/select % [:p.date]))
       (map (comp second (partial re-find #"Date: (.+?)$") first :content))
       (remove nil?)
       first
       to-long))

(defn date-for-file-by-os
  "
  Useful links:
  https://github.com/juxt/yada/blob/master/src/yada/resources/file_resource.clj
  http://stackoverflow.com/questions/2723838/\\
  determine-file-creation-date-in-java
  "
  [path]
  (-> path
      clojure.java.io/file
      .toPath
      (Files/getFileAttributeView PosixFileAttributeView
                                  (into-array java.nio.file.LinkOption
                                              []))
      .readAttributes
      .creationTime
      .toMillis))


(defn date-for-org-file [site-source-dir basename]
  (let [path (format "%s/%s.html" site-source-dir basename)]
    (from-long (or (date-for-org-file-by-header path)
                   (date-for-file-by-os path)))))
