(ns organa.image
  (:require [clojure.string :as string]))

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


