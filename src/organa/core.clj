(ns organa.core
  (:gen-class)
  (:require [clj-rss.core :as rss]
            [clj-time.format :as tformat]
            [clojure.java.shell]
            [clojure.string :as str]
            [clojure.walk]
            [environ.core :refer [env]]
            [garden.core :refer [css] :rename {css to-css}]
            [hiccup.core :as hiccup]
            [net.cgrand.enlive-html :as html]
            [organa.dates :as dates]
            [organa.egg :refer [easter-egg]]
            [organa.html :refer :all]
            [organa.pages :as pages]
            [watchtower.core :refer [watcher rate stop-watch on-add
                                     on-modify on-delete file-filter
                                     extensions]]))

(def home-dir (env :home))
(def remote-host "zerolib.com")
(def site-source-dir (str home-dir "/Dropbox/org/sites/" remote-host))
(def target-dir "/tmp/organa")

(defn ^:private remove-newlines [m]
  (update-in m [:content] (partial remove (partial = "\n"))))

(def ^:private image-file-pattern #"\.png|\.gif$|\.jpg|\.JPG")

(defn ^:private gallery-images [galpath]
  (->> galpath
       clojure.java.io/file
       .listFiles
       (map #(.getName %))
       (filter (partial re-find image-file-pattern))))

(defn inline-gallery [gallery-name]
  (let [galpath (str site-source-dir
                     "/galleries/"
                     gallery-name)]
    (div {} (for [f (gallery-images galpath)
                  :let [img-path (format "./galleries/%s/%s"
                                         gallery-name f)]]
              (a {:href img-path}
                 [(img {:src img-path
                        :class "inline-gallery-thumb"}
                       [])])))))

(defn ^:private execute-organa [m]
  (-> m
      :content
      first
      read-string
      eval))

(defn ^:private year [] (+ 1900 (.getYear (java.util.Date.))))

(defn ^:private footer []
  (p {:class "footer"}
     [(format "© 2006-%d John Jacobsen." (year))
      " Made with "
      (a {:href "https://github.com/eigenhombre/organa"} ["Organa"])
      "."]))

(defn title-for-org-file [parsed-html]
  (-> parsed-html
      (html/select [:h1.title])
      first
      :content
      first
      ;; For some reason ’ is rendering strangely in Chrome when
      ;; synced to zerolib:
      (clojure.string/replace #"’" "'")
      (clojure.string/replace #"…" "...")))

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
     (span {:class (str t "-tag tag")}
           [(a {:href (str t "-blog.html")} t)]))))

(defn rss-links []
  (p ["Subscribe: "
      (a {:href "feed.xml"
          :class "rss"}
         ["RSS feed ... all topics)"])
      " ... or "
      (a {:href "feed.clojure.xml"
          :class "rss"}
         ["Clojure only"])
      " / "
      (a {:href "feed.lisp.xml"
          :class "rss"}
         ["Lisp only"])]))

(defn articles-nav-section [file-name
                            available-files
                            alltags]
  (div
   `(~(a {:name "allposts"} [])
     ~(h2 {:class "allposts"} ["Blog Posts "
                               (span {:class "postcount"}
                                     (->> available-files
                                          (remove :static?)
                                          (remove :draft?)
                                          count
                                          (format "(%d)")))])
     ~(p (concat ["Select from below, "
                  (a {:href "blog.html"} "view all posts")
                  ", or choose only posts for:"]
                 (for [tag (sort alltags)]
                   (span {:class (format "%s-tag tag" tag)}
                         [(a {:href (str tag "-blog.html")}
                             tag)
                          " "]))))
     ~@(when (not= file-name "index")
         [(hr)
          (p [(a {:href "index.html"} [(em ["Home"])])])])
     ~(hr)
     ~@(for [{:keys [file-name date tags]}
             (->> available-files
                  (remove :static?)
                  (remove :draft?)
                  (remove (comp #{file-name} :file-name)))
             :let [parsed-html (parse-org-html site-source-dir file-name)]]
         (when parsed-html
           (p
            (concat
             [(a {:href (str file-name ".html")}
                 [(title-for-org-file parsed-html)])]
             " "
             (tag-markup tags)
             [" "
              (span {:class "article-date"}
                    [(when date (tformat/unparse dates/article-date-format
                                                 date))])]))))
     ~@(when (not= file-name "index")
         [(p [(a {:href "index.html"} [(em ["Home"])])])])
     ~(rss-links))))

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
       (p {:class "prev-next-post"}
          [(span {:class "post-nav-earlier-later"}
                 ["Earlier post "])
           (a {:href (str "./" (:file-name prev-post) ".html")}
              [(:title prev-post)])
           (span (tag-markup (:tags prev-post)))]))
     (when next-post
       [(p {:class "prev-next-post"}
           [(span {:class "post-nav-earlier-later"} ["Later post "])
            (a {:href (str "./" (:file-name next-post) ".html")}
               [(:title next-post)])
            (span (tag-markup (:tags next-post)))])])]))

(defn page-header [css]
  (let [analytics-id (:google-analytics-tracking-id env)
        site-id (:google-analytics-site-id env)]
    [(html/html-snippet (easter-egg))
     (script {:type "text/javascript"
              :src (str "https://cdnjs.cloudflare.com"
                        "/ajax/libs/mathjax/2.7.2/"
                        "MathJax.js"
                        "?config=TeX-MML-AM_CHTML")}
             [])
     (link {:href "./favicon.gif"
            :rel "icon"
            :type "image/gif"}
           [])

     ;; WIP: Responsive website
     ;; ;; Bootstrap
     ;; (link {:rel "stylesheet"
     ;;        :href (str "https://stackpath.bootstrapcdn.com/"
     ;;                   "bootstrap/4.3.1/css/bootstrap.min.css")
     ;;        :integrity (str "sha384-ggOyR0iXCbMQv3Xipma34MD+dH/"
     ;;                        "1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T")
     ;;        :crossorigin "anonymous"}
     ;;       [])

     ;; ;; Bootstrap
     ;; (script {:src "https://code.jquery.com/jquery-3.3.1.slim.min.js"
     ;;          :integrity (str "sha384-q8i/X+965DzO0rT7abK41JStQIAqVg"
     ;;                          "RVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo")
     ;;          :crossorigin "anonymous"})
     ;; ;; Bootstrap
     ;; (script {:src (str "https://cdnjs.cloudflare.com/ajax/libs/"
     ;;                    "popper.js/1.14.7/umd/popper.min.js")
     ;;          :integrity (str "sha384-UO2eT0CpHqdSJQ6hJty5KVphtP"
     ;;                          "hzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1")
     ;;          :crossorigin "anonymous"})
     ;; ;; Bootstrap
     ;; (script {:src (str "https://stackpath.bootstrapcdn.com"
     ;;                    "/bootstrap/4.3.1/js/bootstrap.min.js")
     ;;          :integrity (str "sha384-JjSmVgyd0p3pXB1rRibZUAY"
     ;;                          "oIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM")
     ;;          :crossorigin "anonymous"})

     ;; ;; Bootstrap
     ;; (meta-tag {:name "viewport"
     ;;            :content (str "width=device-width, "
     ;;                          "initial-scale=1, "
     ;;                          "shrink-to-fit=no")}
     ;;           [])

     ;; Analytics
     (script {:type "text/javascript"
              :async true
              :src
              (format "https://www.googletagmanager.com/gtag/js?id=UA-%s-%s"
                      analytics-id site-id)}
             [])
     (script {:type "text/javascript"}
             ["window.dataLayer = window.dataLayer || [];"
              "function gtag(){dataLayer.push(arguments);}"
              "gtag('js', new Date());"
              (format "gtag('config', 'UA-%s-%s');" analytics-id site-id)])
     (style css)]))

(defn transform-enlive [file-name date site-source-dir available-files
                        tags alltags css static? draft? enl]
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
    [:body] remove-newlines
    [:ul] remove-newlines
    [:html] remove-newlines
    [:head] remove-newlines
    [:head] (html/append (page-header css))
    [:pre.src-organa] execute-organa
    [:div#content :h1.title]
    (html/after
        `[~@(concat
             [(when-not static?
                (tag-markup (remove #{"static" "draft"} tags)))
              (p [(span {:class "author"} [(a {:href "index.html"}
                                              ["John Jacobsen"])])
                  (br)
                  (span {:class "article-header-date"}
                        [(tformat/unparse dates/article-date-format date)])])
              (p [(a {:href "index.html"} [(strong ["Home"])])
                  " "
                  (a {:href "blog.html"} ["Other Posts"])])]
             (when-not (or static? draft?)
               (prev-next-tags file-name available-files))
             [(div {:class "hspace"} [])])])
    [:div#content] (html/append
                    (div {:class "hspace"} [])
                    (when-not (or static? draft?)
                      (prev-next-tags file-name available-files))
                    (articles-nav-section file-name
                                          available-files
                                          alltags)
                    (footer))))

(defn process-html-file! [site-source-dir
                          target-dir
                          {:keys [file-name date static? draft? parsed tags]}
                          available-files
                          alltags
                          css]
  (->> parsed
       (transform-enlive file-name
                         date
                         site-source-dir
                         available-files
                         tags
                         alltags
                         css
                         static?
                         draft?)
       html/emit*        ;; turn back into html
       (apply str)
       (spit (str target-dir "/" file-name ".html"))))

(defn html-file-exists [org-file-name]
  (-> org-file-name
      (clojure.string/replace
       #"\.org$" ".html")
      clojure.java.io/file
      .exists))

(defn available-org-files [site-source-dir]
  (->> site-source-dir
       clojure.java.io/file
       file-seq
       (filter (comp #(.endsWith % ".org") str))
       (filter html-file-exists)
       (remove (comp #(.contains % ".#") str))
       (map #(.getName %))
       (map #(.substring % 0 (.lastIndexOf % ".")))))

(defn sh [& cmds]
  (apply clojure.java.shell/sh
         (clojure.string/split (apply str cmds) #"\s+")))

(defn ensure-target-dir-exists! [target-dir]
  ;; FIXME: do it the Java way
  (sh "mkdir -p " target-dir))

(defn stage-site-image-files! [site-source-dir target-dir]
  ;; FIXME: avoid bash hack?
  (doseq [f (->> site-source-dir
                 clojure.java.io/file
                 .listFiles
                 (map #(.toString %))
                 (filter (partial re-find image-file-pattern)))]
    (sh "cp -p " f " " target-dir)))

(defn stage-site-static-files! [site-source-dir target-dir]
  (println "Syncing files in static directory...")
  (apply clojure.java.shell/sh
         (clojure.string/split
          (format "rsync -vurt %s/static %s/galleries %s"
                  site-source-dir site-source-dir target-dir)
          #" ")))

;; FIXME: Hack-y?
(def base-enlive-snippet
  (html/html-snippet "<html><head></head><body></body></html>"))

(defn generate-html-for-galleries! [site-source-dir css]
  (let [galleries-dir (str site-source-dir "/galleries")]
    (doseq [galpath (->> galleries-dir
                         clojure.java.io/file
                         .listFiles
                         (map str)
                         (remove #(.contains % "/.")))
            :let [galfiles (gallery-images galpath)]]
      (println "Making gallery" galpath)
      (->> (html/at base-enlive-snippet
             [:head] (html/append (page-header css))
             [:body]
             (html/append
              [(div {:class "gallery"}
                    (for [f galfiles]
                      (a {:href (str "./" f)}
                         [(img {:src (str "./" f)
                                :height "250px"}
                               [])])))]))
           (html/emit*)
           (apply str)
           (spit (str galpath "/index.html"))))))

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

(defn emit-html-to-file [file-name enlive-tree]
  (->> enlive-tree
       (html/emit*)
       (cons "<!doctype html>\n")
       (apply str)
       (spit (str target-dir "/" file-name))))

(defn make-home-page [site-source-dir org-files tags css]
  (emit-html-to-file
   "index.html"
   (html/at base-enlive-snippet
     [:head] (html/append (page-header css))
     [:body] (html/append
              [(pages/home-body)
               ;;(div {:class "hspace"} [])
               ])
     [:div#blogposts] (html/append
                       [(articles-nav-section "index"
                                              org-files
                                              tags)
                        (footer)]))))

(defn make-blog-page
  ([site-source-dir org-files tags css]
   (make-blog-page :all site-source-dir org-files tags css))
  ([tag site-source-dir org-files tags css]
   (println (format "Making blog page for tag '%s'" tag))
   (let [tag-posts (if (= tag :all)
                     org-files
                     (filter (comp (partial some #{tag}) :tags) org-files))]
     (emit-html-to-file
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
                                                 tags)
                           (footer)]))))))

(defn html-for-rss
  "
  Remove JavaScript and CSS bits from Org-generated HTML for RSS feed.
  "
  [parsed-html]
  (clojure.walk/prewalk
   (fn [x]
     (if (and (map? x)
              (#{"text/css" "text/javascript"} (get-in x [:attrs :type])))
       (dissoc (into {} x) :content)
       x))
   parsed-html))

(defn make-rss-feeds
  ([topic site-source-dir rss-file-name org-files]
   (make-rss-feeds site-source-dir
                   rss-file-name
                   (filter (comp (partial some #{topic})
                                 :tags)
                           org-files)))
  ([site-source-dir rss-file-name org-files]
   (let [rss-file-path (str target-dir "/" rss-file-name)
         posts-for-feed (->> org-files
                             (remove :static?)
                             (remove :draft?)
                             (take 20))
         feed-items (for [f posts-for-feed
                          :let [file-name (:file-name f)
                                local-path (format "%s/%s.html"
                                                   site-source-dir
                                                   file-name)
                                link-path (format "http://%s/%s.html"
                                                  remote-host
                                                  file-name)]]
                      {:title (:title f)
                       :link link-path
                       :pubDate (.toDate (:date f))
                       :description (format "<![CDATA[ %s ]]>"
                                            (->> f
                                                 :parsed
                                                 html-for-rss
                                                 html/emit*
                                                 (apply str)))})]
     (->> feed-items
          (rss/channel-xml {:title "John Jacobsen"
                            :link (str "http://" remote-host)
                            :description "Posts by John Jacobsen"})
          (spit rss-file-path)))))

(defn- collect-org-files [files-to-process]
  (->> (for [f files-to-process]
         (let [parsed-html
               (parse-org-html site-source-dir f)
               tags (tags-for-org-file parsed-html)
               parsed (->> (str f ".html")
                           (str site-source-dir "/")
                           slurp
                           ;; convert to Enlive:
                           html/html-snippet
                           ;; remove useless stuff at top:
                           (drop 3))]
           {:file-name f
            ;; FIXME: don't re-parse for dates!
            :date (dates/date-for-org-file site-source-dir f)
            :title (title-for-org-file parsed-html)
            :tags tags
            :parsed parsed
            :static? (some #{"static"} tags)
            :draft? (some #{"draft"} tags)}))
       (sort-by :date)
       reverse))

(defn- proof-files-to-process [all-org-files]
  (->> all-org-files
       (sort-by (fn [fname]
                  (->> (str fname ".html")
                       (str site-source-dir "/")
                       (dates/date-for-file-by-os))))
       reverse
       (take 10)))

(defn generate-site [& [proof]]
  (println "The party commences....")
  (ensure-target-dir-exists! target-dir)
  (let [css (->> "index.garden"
                 (str site-source-dir "/")
                 load-file
                 to-css)
        all-org-files (available-org-files site-source-dir)
        files-to-process (if-not proof
                           all-org-files
                           (proof-files-to-process all-org-files))
        _ (println (format "Parsing %d HTML'ed Org files..."
                           (count files-to-process)))
        org-files (collect-org-files files-to-process)
        alltags (->> org-files
                     ;; Don't show draft/in progress posts:
                     (remove (comp (partial some #{"draft"}) :tags))
                     (mapcat :tags)
                     (remove #{"static" "draft"})
                     (into #{}))

        image-future (future (stage-site-image-files! site-source-dir
                                                      target-dir))
        static-future (future (generate-html-for-galleries! site-source-dir
                                                            css)
                              (stage-site-static-files! site-source-dir
                                                        target-dir))]

    (make-home-page site-source-dir org-files alltags css)
    (make-blog-page site-source-dir org-files alltags css)
    (doseq [tag alltags]
      (make-blog-page tag site-source-dir org-files alltags css))
    (println "Making RSS feed...")
    (make-rss-feeds site-source-dir "feed.xml" org-files)
    (make-rss-feeds "clojure" site-source-dir "feed.clojure.xml" org-files)
    (make-rss-feeds "lisp" site-source-dir "feed.lisp.xml" org-files)
    (let [page-futures (for [f org-files]
                         (future
                           (process-html-file! site-source-dir
                                               target-dir
                                               f
                                               org-files
                                               alltags
                                               css)))]
      (println "Waiting for static copies to finish...")
      (wait-futures [static-future])
      (println "Waiting for image future to finish...")
      (wait-futures [image-future])
      (println (format "Waiting for %d per-page threads to finish..."
                       (count page-futures)))
      (wait-futures page-futures)
      (println "OK"))))

(defn -main []
  (generate-site)
  (shutdown-agents))
