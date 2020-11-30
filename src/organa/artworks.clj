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
(def target-dir (:target-dir config/config))
(def gallery-html (str target-dir "/artworks.html"))

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

(defn artwork-image-path
  [{:keys [directory artworks-file]}]
  {:pre [directory artworks-file]}
  (oio/path target-dir
            (.getName directory)
            (.getName artworks-file)))

(defn artwork-html-path [{:keys [directory]}]
  (oio/path target-dir
            (.getName directory)
            "index.html"))

(defn artwork-meta-path [{:keys [directory]}]
  (oio/path directory "meta.html"))

(defn artwork-html [artwork]
  (hiccup/html
   [:div
    [:a {:href (artwork-image-path artwork)}
     [:img {:src (artwork-image-path artwork)
            :width 800}]]
    [:pre (with-out-str
            (pprint/pprint artwork #_(dissoc artwork :meta)))]]))

(defn enhance [artwork]
  (let [html-path (artwork-html-path artwork)
        meta-path (artwork-meta-path artwork)
        meta-html (when (.exists (io/file meta-path))
                    (slurp meta-path))
        parsed-meta (html/parse-org-html meta-html)
        title (parse/title-for-org-file parsed-meta)
        meta-table (parse/parsed-org-html->table-metadata parsed-meta)]
    (-> artwork
        (assoc :html-path html-path)
        (merge (when title {:title title})
               meta-table))))

(defn write-files! [{:keys [html-path artworks-file] :as artwork}]
  (io/make-parents html-path)
  (spit html-path (artwork-html artwork))
  (io/copy artworks-file
           (-> html-path
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

(defn artworks-html [artwork-records]
  (hiccup/html
   [:div
    [:table
     (for [{:keys [title year html-path] :as r}
           artwork-records]
       [:tr
        [:td [:div
              [:a {:href html-path}
               [:img {:src (artwork-image-path r)
                      :width 400}]]]]
        [:td [:table
              [:tr [:td [:em (or title "")]]]
              [:tr [:td (or year "")]]]]])]]))

(comment
  (require '[clojure.java.shell :as shell])
  (shell/sh "open" artworks-dir)

  (->> (artworks-dirs)
       (pmap artworks-for-dir)
       (map enhance)
       (map write-files!)
       (sort-by :year)
       reverse
       artworks-html
       (spit gallery-html))

  (clojure.java.shell/sh "open" gallery-html))

