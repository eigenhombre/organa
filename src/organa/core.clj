(ns organa.core
  (:require [clj-time.format :as tformat]
            [environ.core :refer [env]]
            [garden.core :refer [css] :rename {css to-css}]
            [hiccup.core :as hiccup]
            [mount.core :refer [defstate] :as mount]
            [net.cgrand.enlive-html :as html]
            [organa.dates :refer [article-date-format date-for-org-file]]
            [organa.html :refer :all]
            [organa.egg :refer [easter-egg]]
            [watchtower.core :refer [watcher rate stop-watch on-add
                                     on-modify on-delete file-filter
                                     extensions]]))


;; Org / HTML manipulation .................................

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
      first))


(defn tags-for-org-file [parsed-html]
  (mapcat :content
          (-> parsed-html
              (html/select [:span.tag])
              first
              :content)))


;; FIXME: this is hack-y; encode whether a page is static or not in
;; .org file?
(def ^:private static-pages #{"index" "about"})


(defn tag-markup [tags]
  (interleave
   (repeat " ")
   (for [t tags]
     (span {:class (str t "-tag tag")} [t]))))


(defn articles-nav-section [file-name site-source-dir available-files]
  (div
   `(~(a {:name "allposts"} [])
     ~(h2 {:class "allposts"} ["All Posts " (span {:class "postcount"}
                                                  (->> available-files
                                                       (remove static-pages)
                                                       count
                                                       (format "(%d)")))])
     #_{:tag :h2
      :attrs {:class "allposts"}
      :content "All Posts"}
     ~@(when (not= file-name "index")
         [(hr)
          (p [(a {:href "index.html"} [(em ["Home"])])])])
     ~@(for [{:keys [file-name date tags]}
             (->> available-files
                  (remove (comp static-pages :file-name))
                  (remove (comp #{file-name} :file-name)))
             :let [parsed-html (parse-org-html site-source-dir file-name)]]
         (p
          (concat
           [(a {:href (str file-name ".html")}
               [(title-for-org-file parsed-html)])]
           " "
           (tag-markup tags)
           [" "
            (span {:class "article-date"}
                  [(when date (tformat/unparse article-date-format date))])])))
     ~@(when (not= file-name "index")
         [(p [(a {:href "index.html"} [(em ["Home"])])])]))))


(defn position-of-current-file [file-name available-files]
  (->> available-files
       (map-indexed vector)
       (filter (comp (partial = file-name) :file-name second))
       ffirst))


(defn prev-next-tags [file-name available-files]
  (let [files (->> available-files
                   (remove (comp static-pages :file-name))
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


(defn transform-enlive [file-name date site-source-dir available-files css enl]
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
           [:head] (html/append [(html/html-snippet easter-egg)
                                 (style css)])
           [:div#content :h1.title]
           (html/after
               `[~@(when-not (static-pages file-name)
                     (concat
                      [(p [(span {:class "author"} ["John Jacobsen"])
                           (br)
                           (span {:class "article-header-date"}
                                 [(tformat/unparse article-date-format date)])])
                       (p [(a {:href "index.html"} [(strong ["Home"])])
                           " "
                           (a {:href "#allposts"} ["All Posts"])])]
                      (prev-next-tags file-name available-files)
                      [(div {:class "hspace"} [])]))])
           [:div#content] (html/append
                           (div {:class "hspace"} [])
                           (articles-nav-section file-name
                                                 site-source-dir
                                                 available-files)
                           (footer))))


(defn process-org-html [file-name
                        date
                        site-source-dir
                        available-files
                        css
                        txt]
  (->> txt
       html/html-snippet  ;; convert to Enlive
       (drop 3)           ;; remove useless stuff at top
       (transform-enlive file-name date site-source-dir available-files css)
       html/emit*         ;; turn back into html
       (apply str)))


(defn process-html-file! [site-source-dir
                          target-dir
                          {:keys [file-name date]}
                          available-files
                          extra-css]
  (->> (str file-name ".html")
       (str site-source-dir "/")
       slurp
       (process-org-html file-name
                         date
                         site-source-dir
                         available-files extra-css)
       (spit (str target-dir "/" file-name ".html"))))


(defn available-org-files [site-source-dir]
  (->> site-source-dir
       clojure.java.io/file
       file-seq
       (filter (comp #(.endsWith % ".org") str))
       (map #(.getName %))
       (map #(.substring % 0 (.lastIndexOf % ".")))))


(defn sh [& cmds]
  (apply clojure.java.shell/sh
         (clojure.string/split (apply str cmds) #"\s+")))


(defn ensure-target-dir-exists! [target-dir]
  (sh "mkdir -p " target-dir))


(def image-file-pattern #"\.png|\.gif$|\.jpg|\.JPG")


(defn stage-site-image-files! [site-source-dir target-dir]
  (doseq [f (->> site-source-dir
                 clojure.java.io/file
                 .listFiles
                 (map #(.toString %))
                 (filter (partial re-find image-file-pattern)))]
    (sh "cp " f " " target-dir)))


(defn stage-site-static-files! [site-source-dir target-dir]
  (println "Syncing files in static directory...")
  (apply clojure.java.shell/sh
         (clojure.string/split
          (format "rsync -vurt %s/static %s/galleries %s"
                  site-source-dir site-source-dir target-dir)
          #" ")))


(defn generate-html-for-galleries! [site-source-dir]
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
      (let [html-out
            (hiccup/html
             [:div
              [:p (str "Gallery " galpath)]
              [:p (str "(This is a placeholder gallery until "
                       "I can implement something a little "
                       "more polished...)")]
              (for [f galfiles]
                [:a {:href (str "./" f)}
                 [:img {:src (str "./" f), :height "250px"}]])])]
        (spit (str galpath "/index.html") html-out)))))


(defn wait-futures [futures]
  (doseq [fu futures]
    (try
      @fu
      (catch Throwable t
        (.printStackTrace t)))))


(defn generate-static-site [remote-host
                            site-source-dir
                            target-dir]
  (let [css (->> "index.garden"
                 (str site-source-dir "/")
                 load-file
                 to-css)
        org-files (->> (for [f (available-org-files site-source-dir)]
                         (let [parsed-html
                               (parse-org-html site-source-dir f)]
                           {:file-name f
                            ;; FIXME: don't re-parse for dates!
                            :date (date-for-org-file site-source-dir f)
                            :title (title-for-org-file parsed-html)
                            :tags (tags-for-org-file parsed-html)}))
                       (sort-by :date)
                       reverse)
        _ (ensure-target-dir-exists! target-dir)

        image-future
        (future (stage-site-image-files! site-source-dir target-dir))

        static-future
        (future
          (generate-html-for-galleries! site-source-dir)
          (stage-site-static-files! site-source-dir target-dir))]

    (let [futures (doall (for [f org-files]
                           (future
                             (.write *out* (str "Processing "
                                                (:file-name f)
                                                "\n"))
                             (process-html-file! site-source-dir
                                                 target-dir
                                                 f
                                                 org-files
                                                 css))))]
      (println "Waiting for threads to finish...")
      (wait-futures (concat [static-future image-future] futures))
      (println "OK"))))


(def home-dir (env :home))
(def remote-host "zerolib.com")
(def site-source-dir (str home-dir "/Dropbox/org/sites/" remote-host))
(def target-dir "/tmp/organa")


(def key-location (str home-dir "/.ssh/do_id_rsa"))


(defn update-site []
  (generate-static-site remote-host
                        site-source-dir
                        target-dir))


(defstate watcher-state
  :start (let [update-fn (fn [f]
                           (println "added " f)
                           (update-site))]
           (update-site)
           (println "starting watcher...")
           (watcher [site-source-dir]
             (file-filter (extensions :html :garden))
             (file-filter (fn [x]
                            (-> x
                                .getPath
                                (.contains "/galleries/")
                                not)))
             (rate 1000)
             (on-add update-fn)
             (on-delete update-fn)
             (on-modify update-fn)))
  :stop (stop-watch))


(mount/stop)
(mount/start)


(defn sync-tmp-files-to-remote! [key-location
                                 target-dir
                                 remote-host
                                 remote-dir]
  ;; FIXME: Make less hack-y:
  (spit "/tmp/sync-script"
        (format "rsync --rsh 'ssh -i %s' -vurt %s/* root@%s:%s"
                key-location
                target-dir
                remote-host
                remote-dir))
  (sh "bash /tmp/sync-script"))


(comment
  (sh "open file://"  target-dir "/index.html")
  (sync-tmp-files-to-remote! key-location
                             target-dir
                             remote-host
                             (str "/www/" remote-host))
  (sh "open http://" remote-host))
