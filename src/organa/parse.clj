(ns organa.parse
  (:require [clojure.string :as string]
            [net.cgrand.enlive-html :as html]))

(defn title-for-org-file [parsed-html]
  (some-> parsed-html
          (html/select [:h1.title])
          first
          :content
          first
          ;; For some reason ’ is rendering strangely in Chrome when
          ;; synced to zerolib:
          (string/replace #"’" "'")
          (string/replace #"…" "...")))

(defn empty-org-table-entry? [s]
  (let [s-cleaned (clojure.string/replace s #" " "")]
    (empty? s-cleaned)))

(defn clean-empty-string-values [m]
  (into {}
        (for [[k v] m]
          (when-not (empty-org-table-entry? v)
            [k v]))))

(defn parsed-org-html->table-metadata [parsed-html]
  (some->> (html/select parsed-html [:table])
           first
           :content
           (filter (comp (partial = :tbody) :tag))
           first
           :content
           (filter map?)
           (filter (comp (partial = :tr) :tag))
           (map :content)
           (map (partial remove string?))
           (map (partial map :content))
           (mapcat vec)
           (map first)
           (apply hash-map)
           clojure.walk/keywordize-keys
           clean-empty-string-values))

(comment
  (->>
   "/Users/jacobsen/org/sites/zerolib.com/artworks/crows-and-civilization/meta.html"
   slurp
   organa.html/parse-org-html
   parsed-org-html->table-metadata)
  ;;=>
  '{:width " ", :height " ", :medium "Oil on Linen", :year "2019", :price "2000"})
