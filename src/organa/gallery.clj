(ns organa.gallery
  "
  Functions for implementing galleries of images.
  "
  (:require [clojure.java.io :as io]
            [organa.config :as config]
            [organa.html :as h]
            [organa.image :as img])
  (:import [java.io File]))

(defn gallery-images [galpath]
  (->> galpath
       io/file
       .listFiles
       (map #(.getName ^File %))
       sort
       (filter (partial re-find img/image-file-pattern))
       (remove #(.contains ^String % "-thumb.png"))))

(defn inline-gallery
  "
  This is invoked directly from one of the org files on the blog!
  FIXME: sketchy!
  "
  [gallery-name]
  (let [galpath (str (:site-source-dir config/config)
                     "/galleries/"
                     gallery-name)]
    (h/div {} (for [f (gallery-images galpath)
                    :let [img-path (format "./galleries/%s/%s"
                                           gallery-name f)]]
                (h/a {:href img-path}
                     [(h/img {:src img-path
                              :class "inline-gallery-thumb"}
                             [])])))))
