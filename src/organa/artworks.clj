(ns organa.artworks
  (:require [clj-yaml.core :as yaml]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [hiccup.core :as hiccup]
            [mikera.image.core :as image]
            [organa.config :as config]
            [organa.files :as files]
            [organa.html :as html]
            [organa.image :as img]
            [organa.io :as oio]
            [organa.parse :as parse])
  (:import [java.io File]))

;; EXPERIMENTAL

;; FIXME: DRY vs. core.clj (move to config.clj after creating that)
(def site-source-dir (:site-source-dir config/config))
(def image-file-extensions #{"png" "gif" "jpg" "jpeg"})
(def artworks-dir (str site-source-dir "/artworks"))
(def target-dir (str (:target-dir config/config)
                     "/artworks"))
(def gallery-html (str target-dir "/index.html"))

(defn artworks-dirs []
  {:post [(seq %)
          (every? clojure.java.io/file %)]}
  (filter #(.isDirectory ^File %)
          (.listFiles ^File (clojure.java.io/file artworks-dir))))

(defn artwork-file? [^File f]
  {:pre [(= File (type f))]}
  (-> f
      .getName
      (clojure.string/split #"\.")
      last
      image-file-extensions
      boolean))

(def max-thumb-side 600)

(defn artwork-meta-path [{:keys [directory]}]
  (oio/path directory "meta.html"))

(defn artwork-meta-path-yml [{:keys [directory]}]
  (oio/path directory "meta.yml"))

(defn artwork-html [css {:keys [^File artworks-file]
                         :as artwork}]
  (let [artworks-file-name (.getName artworks-file)]
    (hiccup/html
     [:html
      [:head [:style css]]
      [:body
       [:div
        [:a {:href artworks-file-name}
         [:img {:src artworks-file-name
                :width 800}]]
        [:pre (with-out-str
                (pprint/pprint artwork #_(dissoc artwork :meta)))]]]])))

(defn load-meta [artwork]
  ;; Try YAML first, then parsed Org->HTML:
  (let [meta-yml-path (artwork-meta-path-yml artwork)]
    (if (.exists (io/file meta-yml-path))
      (yaml/parse-string (slurp meta-yml-path))
      (let [meta-path (artwork-meta-path artwork)
            meta-html (when (.exists (io/file meta-path))
                        (slurp meta-path))
            parsed-meta (html/parse-org-html meta-html)
            title (parse/title-for-org-file parsed-meta)]
        (merge (parse/parsed-org-html->table-metadata parsed-meta)
               (when title {:title title}))))))

(defn enhance [{:keys [^File directory] :as artwork}]
  (let [dirname (.getName directory)]
    (-> artwork
        (assoc :html-abs-path (oio/path target-dir dirname "index.html")
               :html-rel-path (oio/path dirname "index.html"))
        (merge (load-meta artwork)))))

(defn directory-of-file [f]
  (-> f
      io/file
      oio/dirfile))

(defn write-files! [css {:keys [html-abs-path
                                ^File artworks-file]
                         :as artwork}]
  (io/make-parents html-abs-path)
  (spit html-abs-path (artwork-html css artwork))
  (io/copy artworks-file
           (-> html-abs-path
               directory-of-file
               (oio/path (.getName artworks-file))
               io/file))
  artwork)

(defn artworks-for-dir [^File d]
  (let [artworks-file (first (filter artwork-file?
                                     (.listFiles d)))
        image-object (image/load-image artworks-file)
        height (image/height image-object)
        width (image/width image-object)
        ratio (/ width height)
        thumb-width (if (> width height)
                      max-thumb-side
                      (int (/ max-thumb-side ratio)))
        thumb-height (if (> width height)
                       (int (/ max-thumb-side ratio))
                       max-thumb-side)]
    {:directory d
     :artworks-file artworks-file
     :image-object image-object
     :height height
     :width width
     :thumb-width thumb-width
     :thumb-height thumb-height}))

(defn thumb-name [s]
  (let [[prefix _] (files/splitext s)]
    (str prefix "-thumb.png")))

(defn artworks-html [css recs]
  (hiccup/html
   [:html
    [:head [:style css]]
    [:body
     [:div.margined
      [:table
       (for [{:keys [title
                     year
                     medium
                     directory
                     artworks-file
                     html-rel-path]} recs]
         [:tr
          [:td [:div.artworkspic
                [:a {:href html-rel-path}
                 [:img {:src
                        (oio/path (.getName ^File directory)
                                  (thumb-name
                                   (.getName ^File artworks-file)))
                        :width 200}]]]]
          [:td [:table
                [:tr [:td [:em (or title "")]]]
                [:tr [:td (->> [medium year]
                               (remove nil?)
                               (clojure.string/join ", "))]]]]])]]]]))

(defn thumb-path [html-abs-path artworks-file]
  (oio/path (directory-of-file html-abs-path)
            (thumb-name (str artworks-file))))

;; Could cache or memoize this... but there isn't too much work there,
;; yet...:
(defn make-thumb! [{:keys [artworks-file
                           html-abs-path] :as artwork}]
  (let [tp (thumb-path html-abs-path artworks-file)]
    (io/make-parents tp)
    (img/create-thumbnail! 600 artworks-file tp))
  artwork)

(defn spit-out-artworks-pages! [css]
  (->> (artworks-dirs)
       (pmap artworks-for-dir)
       (map enhance)
       (remove (comp #{"true"} :hidden))
       (pmap make-thumb!)
       (map (partial write-files! css))
       (sort-by (comp str :year))
       reverse
       (artworks-html css)
       (spit gallery-html)))

(comment
  (require '[clojure.java.shell :as shell]
           '[garden.core :refer [css] :rename {css to-css}])
  (shell/sh "open" artworks-dir)

  (let [css (to-css (load-file (str site-source-dir "/" "index.garden")))]
    (spit-out-artworks-pages! css))

  (clojure.java.shell/sh "open" gallery-html))
