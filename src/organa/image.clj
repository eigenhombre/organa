(ns organa.image
  (:require [clojure.string :as string]
            [mikera.image.core :as image]
            [organa.image :as img]))

(def image-extensions ["png" "jpg" "jpeg" "gif"])

(def ^{:doc
       "
       image-file-pattern
       ;;=>
       #\"\\.PNG|\\.JPG|\\.JPEG|\\.GIF|\\.png|\\.jpg|\\.jpeg|\\.gif\"
       "}
  image-file-pattern
  (->> image-extensions
       (concat (map string/upper-case image-extensions))
       (map (partial str "\\."))
       (clojure.string/join "|")
       re-pattern))

(def max-height 250)

(defn create-thumbnail! [orig-path thumb-path]
  (let [img (image/load-image orig-path)
        h (.getHeight img)
        w (.getWidth img)
        new-h max-height
        new-w (int (* w (/ max-height h)))]
    (image/write (image/resize img new-w new-h)
                 thumb-path
                 "png"
                 :quality 1.0)))
