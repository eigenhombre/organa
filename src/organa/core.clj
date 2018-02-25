(ns organa.core
  (:gen-class)
  (:require [clj-time.format :as tformat]
            [clj-rss.core :as rss]
            [clojure.java.shell]
            [clojure.walk]
            [environ.core :refer [env]]
            [garden.core :refer [css] :rename {css to-css}]
            [hiccup.core :as hiccup]
            [net.cgrand.enlive-html :as html]
            [organa.dates :refer [article-date-format date-for-org-file]]
            [organa.pages :as pages]
            [organa.html :refer :all]
            [organa.egg :refer [easter-egg]]
            [watchtower.core :refer [watcher rate stop-watch on-add
                                     on-modify on-delete file-filter
                                     extensions]]))


(def home-dir (env :home))
(def remote-host "zerolib.com")
(def site-source-dir (str home-dir "/Dropbox/org/sites/" remote-host))
(def target-dir "/tmp/organa")


(defn ^:private remove-newlines [m]
  (update-in m [:content] (partial remove (partial = "\n"))))


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


(defn tags-for-org-file [parsed-html]
  (mapcat :content
          (-> parsed-html
              (html/select [:span.tag])
              first
              :content)))


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
         ["Clojure only"])]))


(defn articles-nav-section [file-name
                            site-source-dir
                            available-files
                            tags]
  (div
   `(~(a {:name "allposts"} [])
     ~(h2 {:class "allposts"} ["Posts "
                               (span {:class "postcount"}
                                     (->> available-files
                                          (remove :static?)
                                          (remove :draft?)
                                          count
                                          (format "(%d)")))])
     ~(p (concat ["Select from below, "
                  (a {:href "blog.html"} "view all posts")
                  ", or choose only posts for:"]
                 (for [tag (sort tags)]
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
                    [(when date (tformat/unparse article-date-format
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
    [(html/html-snippet easter-egg)
     (script {:type "text/javascript"
              :src (str "https://cdnjs.cloudflare.com"
                        "/ajax/libs/mathjax/2.7.2/"
                        "MathJax.js"
                        "?config=TeX-MML-AM_CHTML")}
             [])
     (link {:href "./favicon.gif"
            :rel "icon"
            :type "image/gif"} [])
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
    [:body] remove-newlines
    [:ul] remove-newlines
    [:html] remove-newlines
    [:head] remove-newlines
    [:head] (html/append (page-header css))
    [:div#content :h1.title]
    (html/after
        `[~@(concat
             [(tag-markup tags)
              (p [(span {:class "author"} ["John Jacobsen"])
                  (br)
                  (span {:class "article-header-date"}
                        [(tformat/unparse article-date-format date)])])
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
                                          site-source-dir
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


(def image-file-pattern #"\.png|\.gif$|\.jpg|\.JPG")


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
            :let [galfiles
                  (->> galpath
                       clojure.java.io/file
                       .listFiles
                       (map #(.getName %))
                       (filter (partial re-find image-file-pattern)))]]
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
  (doseq [fu futures]
    (try
      (deref fu)
      (catch Throwable t
        (println t)))))


(defn make-home-page [site-source-dir org-files tags css]
  (let [out-path (str target-dir "/index.html")]
    (->> (html/at base-enlive-snippet
           [:head] (html/append (page-header css))
           [:body] (html/append
                    [(pages/home-body)
                     ;;(div {:class "hspace"} [])
                     ])
           [:div#blogposts] (html/append
                             [(articles-nav-section "index"
                                                    site-source-dir
                                                    org-files
                                                    tags)
                              (footer)]))
         (html/emit*)
         (apply str)
         (spit out-path))))


(defn make-blog-page
  ([site-source-dir org-files tags css]
   (make-blog-page :all site-source-dir org-files tags css))
  ([tag site-source-dir org-files tags css]
   (println (format "Making blog page for tag '%s'" tag))
   (let [out-path (str target-dir
                       "/"
                       (if (= tag :all) "" (str tag "-"))
                       "blog.html")
         tag-posts (if (= tag :all)
                     org-files
                     (filter (comp (partial some #{tag}) :tags) org-files))]
     (->> (html/at base-enlive-snippet
            [:head] (html/append (page-header css))
            [:body] (html/append
                     [(pages/blog-body)
                      ;;(div {:class "hspace"} [])
                      ])
            [:div#blogposts] (html/append
                              [(articles-nav-section "blog"
                                                     site-source-dir
                                                     tag-posts
                                                     tags)
                               (footer)]))
          (html/emit*)
          (apply str)
          (spit out-path)))))


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


(defn generate-site [remote-host
                     site-source-dir
                     target-dir]
  (let [css (->> "index.garden"
                 (str site-source-dir "/")
                 load-file
                 to-css)
        org-files (->> (for [f (available-org-files site-source-dir)]
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
                            :date (date-for-org-file site-source-dir f)
                            :title (title-for-org-file parsed-html)
                            :tags tags
                            :parsed parsed
                            :static? (some #{"static"} tags)
                            :draft? (some #{"draft"} tags)}))
                       (sort-by :date)
                       reverse)
        alltags (->> org-files
                     ;; Don't show draft/in progress posts:
                     (remove (comp (partial some #{"draft"}) :tags))
                     (mapcat :tags)
                     (remove #{"static" "draft"})
                     (into #{}))
        _ (ensure-target-dir-exists! target-dir)

        image-future
        (future (stage-site-image-files! site-source-dir target-dir))
        static-future
        (future
          (generate-html-for-galleries! site-source-dir css)
          (stage-site-static-files! site-source-dir target-dir))]

    (make-home-page site-source-dir org-files alltags css)
    (make-blog-page site-source-dir org-files alltags css)
    (doseq [tag alltags]
      (make-blog-page tag site-source-dir org-files alltags css))
    (println "Making RSS feed...")
    (make-rss-feeds site-source-dir "feed.xml" org-files)
    (make-rss-feeds "clojure" site-source-dir "feed.clojure.xml" org-files)
    (let [futures (doall (for [f org-files]
                           (future
                             (process-html-file! site-source-dir
                                                 target-dir
                                                 f
                                                 org-files
                                                 alltags
                                                 css))))]
      (Thread/sleep 2000)
      (println "Waiting for threads to finish...")
      (wait-futures (concat [static-future image-future] futures))
      (println "OK"))))


(defn update-site []
  (generate-site remote-host
                 site-source-dir
                 target-dir))


(defn -main []
  (println (format  "Creating file://%s/index.html" target-dir))
  (update-site)
  (shutdown-agents))
