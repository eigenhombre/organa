(ns organa.dates
  (:require [net.cgrand.enlive-html :as html]
            [clj-time.coerce :refer [from-long to-long]]
            [clojure.java.io :as io]
            [clj-time.format :as tformat])
  (:import [java.nio.file Files]
           [java.nio.file.attribute PosixFileAttributeView]
           [java.util Calendar]))

(def ^{:doc "Date format used when rendering blog posts"}
  article-date-format (tformat/formatter "EEEE, MMMM d, yyyy"))

(defn ^:private date-for-org-file-by-header [path]
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
  Get \"update\" date and time in milliseconds for a file on the file
  system.

  Useful links:
  https://github.com/juxt/yada/blob/master/src/yada/resources/file_resource.clj
  http://stackoverflow.com/questions/2723838/\\
  determine-file-creation-date-in-java
  "
  {:doc/format :markdown}
  [path]
  (.toMillis
   ^java.nio.file.attribute.FileTime
   (.creationTime
    ^sun.nio.fs.UnixFileAttributes
    (.readAttributes
     ^sun.nio.fs.UnixFileAttributeViews$Posix
     (Files/getFileAttributeView ^sun.nio.fs.UnixPath (.toPath (io/file path))
                                 PosixFileAttributeView
                                 (into-array java.nio.file.LinkOption
                                             []))))))

(defn date-for-org-file
  "
  Find Joda `DateTime` for an Org post in HTML format, first looking
  in the Org Mode header, and if not present there, using the filesystem
  time.
  "
  {:doc/format :markdown}
  [site-source-dir basename]
  (let [path (format "%s/%s.html" site-source-dir basename)]
    (from-long (or (date-for-org-file-by-header path)
                   (date-for-file-by-os path)))))

(defn date->year
  "
  Find year for a `clj-time` time (Joda `DateTime`).
  "
  {:doc/format :markdown}
  [d]
  (let [cal (Calendar/getInstance)]
    (.setTime cal d)
    (.get cal Calendar/YEAR)))

(defn ^Integer current-year
  "
  Current year (at the time the program is being run).
  "
  {:doc/format :markdown}
  []
  (date->year (java.util.Date.)))
