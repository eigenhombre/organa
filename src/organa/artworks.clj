(ns organa.artworks
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [environ.core :refer [env]]
            [hiccup.core :as hiccup]
            [mikera.image.core :as image]
            [organa.config :as config]
            [organa.html :as html]
            [organa.io :as oio]
            [organa.parse :as parse]))

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
  (->> artworks-dir
       clojure.java.io/file
       .listFiles
       (filter #(.isDirectory %))))

(defn artwork-file? [f]
  {:pre [(= java.io.File (type f))]}
  (-> f
      .getName
      (clojure.string/split #"\.")
      last
      image-file-extensions
      boolean))

(def max-thumb-side 600)

(defn artwork-meta-path [{:keys [directory]}]
  (oio/path directory "meta.html"))

(defn artwork-html [css {:keys [artworks-file] :as artwork}]
  (hiccup/html
   [:html
    [:head [:style css]]
    [:body
     [:div
      [:a {:href (.getName artworks-file)}
       [:img {:src (.getName artworks-file)
              :width 800}]]
      [:pre (with-out-str
              (pprint/pprint artwork #_(dissoc artwork :meta)))]]]]))

(defn enhance [{:keys [directory] :as artwork}]
  (let [meta-path (artwork-meta-path artwork)
        meta-html (when (.exists (io/file meta-path))
                    (slurp meta-path))
        parsed-meta (html/parse-org-html meta-html)
        title (parse/title-for-org-file parsed-meta)
        meta-table (parse/parsed-org-html->table-metadata parsed-meta)]
    (-> artwork
        (assoc :html-abs-path (oio/path target-dir
                                        (.getName directory)
                                        "index.html")
               :html-rel-path (oio/path (.getName directory)
                                        "index.html"))
        (merge (when title {:title title})
               meta-table))))

(defn write-files! [css {:keys [html-abs-path artworks-file] :as artwork}]
  (io/make-parents html-abs-path)
  (spit html-abs-path (artwork-html css artwork))
  (io/copy artworks-file
           (-> html-abs-path
               io/file
               oio/dirfile
               (oio/path (.getName artworks-file))
               io/file))
  artwork)

(defn artworks-for-dir [d]
  (let [artworks-file (first (filter artwork-file? (.listFiles d)))
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

(defn artworks-html [css recs]
  (hiccup/html
   [:html
    [:head [:style css]]
    [:body
     [:div.margined
      [:table
       (for [{:keys [title
                     year
                     directory
                     artworks-file
                     html-rel-path] :as r} recs]
         [:tr
          [:td [:div
                [:a {:href html-rel-path}
                 [:img {:src (oio/path (.getName directory)
                                       (.getName artworks-file))
                        :width 400}]]]]
          [:td [:table
                [:tr [:td [:em (or title "")]]]
                [:tr [:td (or year "")]]]]])]]]]))

(defn spit-out-artworks-pages! [css]
  (->> (artworks-dirs)
       (pmap artworks-for-dir)
       (map enhance)
       (map (partial write-files! css))
       (sort-by :year)
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
