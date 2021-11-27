(ns organa.core
  (:gen-class)
  (:require [clj-time.format :as tformat]
            [clojure.java.io :as io]
            [clojure.java.shell]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.string :as string]
            [clojure.walk]
            [environ.core :refer [env]]
            [garden.core :refer [css] :rename {css to-css}]
            [net.cgrand.enlive-html :as html]
            [organa.artworks :as artworks]
            [organa.config :refer [config]]
            [organa.dates :as dates]
            [organa.egg :refer [easter-egg]]
            [organa.fs :as fs]
            [organa.gallery :as gal]
            [organa.html :as h]
            [organa.image :as img]
            [organa.pages :as pages]
            [organa.parse :as parse]
            [organa.rss :as rss])
  (:import [java.io File]))

(defn ^:private remove-newline-strings [coll]
  (remove (partial = "\n") coll))

(defn ^:private content-remove-newline-strings
  "
  Remove newlines from `:content` portion of Enlive element `el`.
  "
  [el]
  (update-in el [:content] remove-newline-strings))

(defn ^:private execute-organa [m]
  (-> m
      :content
      first
      read-string
      eval))

(defn ^:private footer []
  (h/p {:class "footer"}
       [(format "© 2006-%d John Jacobsen." (dates/year))
        " Made with "
        (h/a {:href "https://github.com/eigenhombre/organa"} ["Organa"])
        "."]))

;; Org mode exports changed how tags in section headings are handled...
(defn tags-for-org-file-via-old-span-tag [parsed-html]
  (-> parsed-html
      (html/select [:span.tag])
      first
      :content
      (#(mapcat :content %))))

(defn- tags-bracketed-by-colons
  "

      (tags-bracketed-by-colons \":foo:baz:\")
      ;;=> [\"foo\" \"baz\"]
      (tags-bracketed-by-colons \"adfkljhsadf\")
      ;;=> nil
  "
  [s]
  (some-> (re-find #"^\:(.+?)\:$" s)
          second
          (clojure.string/split
           #":")))

;; Org mode exports changed how tags in section headings are handled...
(defn tags-for-org-file-using-h2 [parsed-html]
  (->> (html/select parsed-html [:h2])
       (mapcat (comp tags-bracketed-by-colons first :content))
       (remove nil?)))

;; Org mode exports changed how tags in section headings are handled...
(defn tags-for-org-file [parsed-html]
  (concat
   (tags-for-org-file-via-old-span-tag parsed-html)
   (tags-for-org-file-using-h2 parsed-html)))

(defn tag-markup [tags]
  (interleave
   (repeat " ")
   (for [t tags]
     (h/span {:class (str t "-tag tag")}
             [(h/a {:href (str t "-blog.html")} t)]))))

(defn articles-nav-section [file-name
                            available-files
                            alltags
                            parsed-org-file-map]
  (h/div
   `(~(h/a {:name "allposts"} [])
     ~(h/h2 {:class "allposts"} ["Blog Posts "
                                 (h/span {:class "postcount"}
                                         (->> available-files
                                              (remove :static?)
                                              (remove :draft?)
                                              count
                                              (format "(%d)")))])
     ~(h/p (concat ["Select from below, "
                    (h/a {:href "blog.html"} "view all posts")
                    ", or choose only posts for:"]
                   (for [tag (sort alltags)]
                     (h/span {:class (format "%s-tag tag" tag)}
                             [(h/a {:href (str tag "-blog.html")}
                                   tag)
                              " "]))))
     ~@(when (not= file-name "index")
         [(h/hr)
          (h/p [(h/a {:href "index.html"} [(h/em ["Home"])])])])
     ~(h/hr)
     ~@(for [{:keys [file-name date tags]}
             (->> available-files
                  (remove :static?)
                  (remove :draft?)
                  (remove (comp #{file-name} :file-name)))
             :let [parsed-html
                   (->> file-name
                        parsed-org-file-map
                        :parsed-html)]]
         (when parsed-html
           (h/p
            (concat
             [(h/a {:href (str file-name ".html")}
                   [(parse/title-for-org-file parsed-html)])]
             " "
             (tag-markup tags)
             [" "
              (h/span {:class "article-date"}
                      [(when date (tformat/unparse dates/article-date-format
                                                   date))])]))))
     ~@(when (not= file-name "index")
         [(h/p [(h/a {:href "index.html"} [(h/em ["Home"])])])])
     ~(rss/rss-links))))

(defn position-of-current-file [file-name available-files]
  (->> available-files
       (map-indexed vector)
       (filter (comp (partial = file-name) :file-name second))
       ffirst))

(defn prev-next-tags [file-name available-files]
  (let [files (->> available-files
                   (remove :static?)
                   (remove :draft?)
                   vec)
        current-pos (position-of-current-file file-name files)
        next-post (get files (dec current-pos))
        prev-post (get files (inc current-pos))]
    [(when prev-post
       (h/p {:class "prev-next-post"}
            [(h/span {:class "post-nav-earlier-later"}
                     ["Earlier post "])
             (h/a {:href (str "./" (:file-name prev-post) ".html")}
                  [(:title prev-post)])
             (h/span (tag-markup (:tags prev-post)))]))
     (when next-post
       [(h/p {:class "prev-next-post"}
             [(h/span {:class "post-nav-earlier-later"} ["Later post "])
              (h/a {:href (str "./" (:file-name next-post) ".html")}
                   [(:title next-post)])
              (h/span (tag-markup (:tags next-post)))])])]))

(defn page-header [css]
  (let [analytics-id (:google-analytics-tracking-id env)
        site-id (:google-analytics-site-id env)]
    [(html/html-snippet (easter-egg))
     (h/script {:type "text/javascript"
                :src (str "https://cdnjs.cloudflare.com"
                          "/ajax/libs/mathjax/2.7.2/"
                          "MathJax.js"
                          "?config=TeX-MML-AM_CHTML")}
               [])
     (h/link {:href "./favicon.gif"
              :rel "icon"
              :type "image/gif"}
             [])

     ;; Analytics
     (h/script {:type "text/javascript"
                :async true
                :src
                (format "https://www.googletagmanager.com/gtag/js?id=UA-%s-%s"
                        analytics-id site-id)}
               [])
     (h/script {:type "text/javascript"}
               ["window.dataLayer = window.dataLayer || [];"
                "function gtag(){dataLayer.push(arguments);}"
                "gtag('js', new Date());"
                (format "gtag('config', 'UA-%s-%s');" analytics-id site-id)])
     (h/style css)]))

(defn transform-enlive [file-name date available-files parsed-org-file-map
                        tags alltags css static? draft? enl]
  (let [prev-next-tags (when-not (or static? draft?)
                         (prev-next-tags file-name available-files))
        nav-section (articles-nav-section file-name
                                          available-files
                                          alltags
                                          parsed-org-file-map)]
    (html/at enl
      [:head :style] nil
      [:head :script] nil
      [:div#postamble] nil
      ;; Old org mode:
      ;; Remove dummy header lines containting tags, in first
      ;; sections:
      [:h2#sec-1] (fn [thing]
                    (when-not (-> thing
                                  :content
                                  second
                                  :attrs
                                  :class
                                  (= "tag"))
                      thing))
      ;; New org mode:
      ;; Remove dummy header lines containing tags:
      [:h2] (fn [thing]
              (when-not (-> thing
                            :content
                            first
                            tags-bracketed-by-colons)
                thing))
      [:body] content-remove-newline-strings
      [:ul] content-remove-newline-strings
      [:html] content-remove-newline-strings
      [:head] content-remove-newline-strings
      [:head] (html/append (page-header css))
      [:pre.src-organa] execute-organa
      [:div#content :h1.title]
      (html/after
          `[~@(concat
               [(when-not static?
                  (tag-markup (remove #{"static" "draft"} tags)))
                (h/p [(h/span {:class "author"} [(h/a {:href "index.html"}
                                                      ["John Jacobsen"])])
                      (h/br)
                      (h/span {:class "article-header-date"}
                              [(tformat/unparse dates/article-date-format
                                                date)])])
                (h/p [(h/a {:href "index.html"} [(h/strong ["Home"])])
                      " "
                      (h/a {:href "blog.html"} ["Other Posts"])])]
               prev-next-tags
               [(h/div {:class "hspace"} [])])])
      [:div#content] (html/append
                      (h/div {:class "hspace"} [])
                      prev-next-tags
                      nav-section
                      (footer)))))

(defn as-string [x]
  (with-out-str
    (pprint/pprint x)))

(defn process-html-file! [{:keys [target-dir]}
                          {:keys [file-name date static? draft?
                                  parsed-html tags unparsed-html] :as r}
                          available-files
                          alltags
                          css
                          parsed-org-file-map]
  (->> parsed-html
       (transform-enlive file-name
                         date
                         available-files
                         parsed-org-file-map
                         tags
                         alltags
                         css
                         static?
                         draft?)
       html/emit*        ;; turn back into html
       (apply str)
       (spit (str target-dir "/" file-name ".html")))
  (->> r
       as-string
       (spit (str target-dir "/" file-name ".edn"))))

(defn html-file-exists [org-file-name]
  (-> org-file-name
      (clojure.string/replace
       #"\.org$" ".html")
      clojure.java.io/file
      .exists))

(defn available-org-files [site-source-dir]
  (->> (fs/files-in-directory site-source-dir :as :file)
       (filter (comp #(.endsWith ^String % ".org") str))
       (filter html-file-exists)
       (remove (comp #(.contains ^String % ".#") str))
       (map #(.getName ^File %))
       (map #(.substring ^String % 0 (.lastIndexOf ^String % ".")))))

(defn sh [& cmds]
  (apply clojure.java.shell/sh
         (clojure.string/split (string/join cmds) #"\s+")))

(defn ensure-target-dir-exists! [target-dir]
  ;; FIXME: do it the Java way
  (sh "mkdir -p " target-dir))

(defn stage-site-image-files! [site-source-dir target-dir]
  ;; FIXME: avoid bash hack?
  (doseq [f (filter (partial re-find img/image-file-pattern)
                    (fs/files-in-directory site-source-dir :as :str))]
    (sh "cp -p " f " " target-dir)))

(defn stage-site-static-files! [site-source-dir target-dir]
  (println "Syncing files in static directory...")
  (apply clojure.java.shell/sh
         (clojure.string/split
          (format "rsync -vurt %s/static %s/galleries %s/artworks %s"
                  site-source-dir
                  site-source-dir
                  site-source-dir
                  target-dir)
          #" ")))

;; FIXME: Hack-y?
(def base-enlive-snippet
  (html/html-snippet "<html><head></head><body></body></html>"))

(defn ^:private galleries-path [site-source-dir]
  (str site-source-dir "/galleries"))

(defn generate-thumbnails-for-gallery! [galpath imagefiles]
  (doseq [img (remove #(.contains ^String % "-thumb")
                      imagefiles)
          :let [[base _] (fs/splitext img)
                thumb-path (format "%s/%s-thumb.png" galpath base)
                orig-path (format "%s/%s" galpath img)]]
    (when-not (.exists (io/file thumb-path))
      (printf "Creating thumbnail file %s from %s...\n"
              thumb-path
              orig-path)
      (img/create-thumbnail! orig-path thumb-path))))

(defn generate-thumbnails-in-galleries! [site-source-dir]
  (let [galleries-dir (galleries-path site-source-dir)]
    (doseq [galpath (fs/files-in-directory galleries-dir :as :str)
            :let [imagefiles (gal/gallery-images galpath)]]
      (printf "Making thumbnails for gallery '%s'\n" (fs/basename galpath))
      (generate-thumbnails-for-gallery! galpath imagefiles))))

(defn gallery-html [css galfiles]
  (->> (html/at base-enlive-snippet
         [:head] (html/append (page-header css))
         [:body]
         (html/append
          [(h/div {:class "gallery"}
                  (for [f galfiles
                        :let [[base _] (fs/splitext f)
                              thumb-path (format "%s-thumb.png" base)]]
                    (h/a {:href (str "./" f)}
                         [(h/img {:src (str "./" thumb-path)
                                  :height "250px"}
                                 [])])))]))
       (html/emit*)
       (apply str)))

(defn generate-html-for-galleries! [site-source-dir css]
  (let [galleries-dir (galleries-path site-source-dir)]
    (doseq [galpath (fs/files-in-directory galleries-dir :as :str)
            :let [galfiles (gal/gallery-images galpath)]]
      (printf "Making gallery '%s'\n" (fs/basename galpath))
      (spit (str galpath "/index.html")
            (gallery-html css galfiles)))))

(defn wait-futures [futures]
  (loop [cnt 0]
    (let [new-cnt (count (filter realized? futures))]
      (when-not (every? realized? futures)
        (Thread/sleep 500)
        (if (= cnt new-cnt)
          (recur cnt)
          (do
            (println new-cnt "completed...")
            (recur new-cnt))))))
  (doseq [fu futures]
    (try
      (deref fu)
      (catch Throwable t
        (println t)))))

(defn emit-html-to-file [target-dir file-name enlive-tree]
  (->> enlive-tree
       (html/emit*)
       (cons "<!doctype html>\n")
       (apply str)
       (spit (str target-dir "/" file-name))))

(defn make-old-home-page [{:keys [target-dir]}
                          org-files
                          parsed-org-file-map
                          tags
                          css]
  (emit-html-to-file
   target-dir
   "index-old.html"
   (html/at base-enlive-snippet
     [:head] (html/append (page-header css))
     [:body] (html/append
              [(pages/home-body)
               ;;(div {:class "hspace"} [])
               ])
     [:div#blogposts] (html/append
                       [(articles-nav-section "index"
                                              org-files
                                              tags
                                              parsed-org-file-map)
                        (footer)]))))

(defn make-blog-page
  ([config org-files parsed-org-file-map tags css]
   (make-blog-page config :all org-files parsed-org-file-map tags css))
  ([{:keys [target-dir]} tag org-files parsed-org-file-map tags css]
   (println (format "Making blog page for tag '%s'" tag))
   (let [tag-posts (if (= tag :all)
                     org-files
                     (filter (comp (partial some #{tag}) :tags) org-files))]
     (emit-html-to-file
      target-dir
      (str (if (= tag :all) "" (str tag "-"))
           "blog.html")
      (html/at base-enlive-snippet
        [:head] (html/append (page-header css))
        [:body] (html/append
                 [(pages/blog-body)
                  ;;(div {:class "hspace"} [])
                  ])
        [:div#blogposts] (html/append
                          [(articles-nav-section "blog"
                                                 tag-posts
                                                 tags
                                                 parsed-org-file-map)
                           (footer)]))))))

(defn remove-gnu-junk [snippet]
  (html/at snippet
    [:script] nil
    [:style] nil))

(defn files->parsed [files-to-process]
  (let [ssd (:site-source-dir config)]
    (into {}
          (for [f files-to-process]
            (let [parsed-html (->> f
                                   (h/parse-org-html ssd)
                                   remove-gnu-junk)
                  tags (tags-for-org-file parsed-html)]
              [f
               ;; FIXME: It's hacky to have f in both places
               {:file-name f
                ;; FIXME: don't re-parse for dates!
                :date (dates/date-for-org-file ssd f)
                :title (parse/title-for-org-file parsed-html)
                :tags tags
                :parsed-html parsed-html
                :unparsed-html (apply str (html/emit* parsed-html))
                :static? (some #{"static"} tags)
                :draft? (some #{"draft"} tags)}])))))

(defn- proof-files-to-process [all-org-files]
  (->> all-org-files
       (sort-by (fn [fname]
                  (->> (str fname ".html")
                       (str (:site-source-dir config) "/")
                       (dates/date-for-file-by-os))))
       reverse
       (take 10)))

(defn generate-site [{:keys [target-dir proof?] :as config}]
  (println "The party commences....")
  (ensure-target-dir-exists! target-dir)
  (let [ssd (:site-source-dir config)
        css (->> (str ssd "/index.garden")
                 load-file
                 to-css)
        all-org-files (available-org-files ssd)
        files-to-process (if-not proof?
                           all-org-files
                           (proof-files-to-process all-org-files))
        _ (println (format "Parsing %d HTML'ed Org files..."
                           (count files-to-process)))
        parsed-org-file-map (files->parsed files-to-process)
        org-files (->> parsed-org-file-map
                       vals
                       (sort-by :date)
                       reverse)
        alltags (->> org-files
                     ;; Don't show draft/in progress posts:
                     (remove (comp (partial some #{"draft"}) :tags))
                     (mapcat :tags)
                     (remove #{"static" "draft"})
                     (into #{}))
        _ (generate-thumbnails-in-galleries! ssd)
        image-future (future (stage-site-image-files! ssd
                                                      target-dir))
        static-future (future (generate-html-for-galleries! ssd
                                                            css)
                              (stage-site-static-files! ssd
                                                        target-dir))]
    (println "Making artworks pages...")
    (artworks/spit-out-artworks-pages! css)
    (make-old-home-page config org-files parsed-org-file-map alltags css)
    (make-blog-page config org-files parsed-org-file-map alltags css)
    (doseq [tag alltags]
      (make-blog-page config tag org-files parsed-org-file-map alltags css))
    (println "Making RSS feed...")
    (rss/make-rss-feeds "feed.xml" org-files)
    (rss/make-rss-feeds "clojure" "feed.clojure.xml" org-files)
    (rss/make-rss-feeds "lisp" "feed.lisp.xml" org-files)
    (let [page-futures (for [f org-files]
                         (future
                           (process-html-file! config
                                               f
                                               org-files
                                               alltags
                                               css
                                               parsed-org-file-map)))]
      (println "Waiting for static copies to finish...")
      (wait-futures [static-future])
      (println "Waiting for image future to finish...")
      (wait-futures [image-future])
      (println (format "Waiting for %d per-page threads to finish..."
                       (count page-futures)))
      (wait-futures page-futures)
      (println "OK"))))

(defn -main []
  (generate-site config)
  (shutdown-agents))

(comment
  (require '[marginalia.core :as marg])
  (marg/run-marginalia ["src/organa/core.clj"])
  (clojure.java.shell/sh "open" "docs/uberdoc.html")
  (generate-site config)
  (generate-site (assoc config :proof? true))
  (clojure.java.shell/sh "open" "/tmp/organa/index-old.html")
  )
