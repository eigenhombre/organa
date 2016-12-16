(ns organa.core
  (:require [clj-time.format :as tformat]
            [environ.core :refer [env]]
            [watchtower.core :refer [watcher rate stop-watch on-add
                                     on-modify on-delete file-filter
                                     extensions]]
            [garden.core :refer [css] :rename {css to-css}]
            [net.cgrand.enlive-html :as html]
            [mount.core :refer [defstate] :as mount]
            [organa.dates :refer [article-date-format date-for-org-file]]))


;; Org / HTML manipulation .................................

(defn ^:private remove-newlines [m]
  (update-in m [:content] (partial remove (partial = "\n"))))


(defn ^:private year [] (+ 1900 (.getYear (java.util.Date.))))


(defn ^:private footer []
  {:tag :p
   :attrs {:class "footer"}
   :content [(format "© %d John Jacobsen." (year))
             " Made with "
             {:tag :a
              :attrs {:href "https://github.com/eigenhombre/organa"}
              :content ["Organa"]}
             "."]})


(defn title-for-org-file [site-source-dir basename]
  (-> (format "%s/%s.html" site-source-dir basename)
      slurp
      html/html-snippet
      (html/select [:h1.title])
      first
      :content
      first))


(defn articles-nav-bar [file-name site-source-dir available-files]
  {:tag :div
   :content
   `({:tag :h2
      :content "More"}
     ~@(when (not= file-name "index")
         [{:tag :hr}
          {:tag :p
           :content [{:tag :a
                      :attrs {:href "index.html"}
                      :content ["Home"]}]}])
     ~@(for [{:keys [file-name date]}
             (->> available-files
                  (remove (comp #{"index"} :file-name))
                  (remove (comp #{file-name} :file-name)))]
         {:tag :p
          :content [{:tag :a
                     :attrs {:href (str file-name ".html")}
                     :content [(title-for-org-file site-source-dir file-name)]}
                    " "
                    {:tag :span
                     :attrs {:class "article-date"}
                     :content [(tformat/unparse
                                article-date-format date)]}]}))})


(defn transform-enlive [file-name date site-source-dir available-files css enl]
  (html/at enl
           [:head :style] nil
           [:head :script] nil
           [:div#postamble] nil
           [:body] remove-newlines
           [:ul] remove-newlines
           [:html] remove-newlines
           [:head] remove-newlines
           [:head] (html/append [{:tag :style
                                  :content css}])
           [:div#content :h1.title]
           (html/after
               [{:tag :p
                 :attrs {:class "article-header-date"}
                 :content `[~@(when-not (= file-name "index")
                                [(tformat/unparse article-date-format date)
                                 {:tag :p
                                  :content [{:tag :a
                                             :attrs {:href "index.html"}
                                             :content ["Home"]}]}])]}])
           [:div#content] (html/append
                           (articles-nav-bar file-name
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


(defn stage-site-image-files! [site-source-dir target-dir]
  (doseq [f (->> site-source-dir
                 clojure.java.io/file
                 file-seq
                 (map #(.toString %))
                 (filter (partial re-find #"\.png|\.gif$")))]
    (sh "cp " f " " target-dir)))


(defn generate-static-site [remote-host
                            site-source-dir
                            target-dir]
  (let [css (->> "index.garden"
                 (str site-source-dir "/")
                 load-file
                 to-css)
        org-files (->> (for [f (available-org-files site-source-dir)]
                         {:file-name f
                          :date (date-for-org-file site-source-dir f)})
                       (sort-by :date)
                       reverse)]
    (ensure-target-dir-exists! target-dir)
    (stage-site-image-files! site-source-dir target-dir)
    (doseq [f org-files]
      (process-html-file! site-source-dir
                          target-dir
                          f
                          org-files
                          css))))


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
                           (println "added" f)
                           (update-site))]
           (update-site)
           (watcher [site-source-dir]
             (file-filter (extensions :html))
             (rate 1000)
             (on-add update-fn)
             (on-delete update-fn)
             (on-modify update-fn)))
  :stop (stop-watch))


(mount/stop)
(mount/start)


(comment
  (sh "open file://"  target-dir "/index.html")
  (sync-tmp-files-to-remote! key-location
                             target-dir
                             remote-host
                             (str "/www/" remote-host))
  (sh "open http://" remote-host))
