(ns organa.core
  (:require [net.cgrand.enlive-html :as html]
            [environ.core :refer [env]]
            [garden.core :refer [css] :rename {css to-css}]))


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


(defn articles-nav-bar [site-source-dir available-files]
  {:tag :div
   :content
   (list* {:tag :p
           :content [{:tag :a
                      :attrs {:href "index.html"}
                      :content ["Home"]}]}
          {:tag :hr}
          {:tag :p
           :content ["Other articles"]}
          (for [f (remove #{"index"} available-files)]
            {:tag :p
             :content [{:tag :a
                        :attrs {:href (str f ".html")}
                        :content [(title-for-org-file site-source-dir f)]}]}))})


(defn transform-enlive [site-source-dir available-files css enl]
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
           [:div#content] (html/append
                           (articles-nav-bar site-source-dir
                                             available-files)
                           (footer))))


(defn process-org-html [site-source-dir available-files css txt]
  (->> txt
       html/html-snippet  ;; convert to Enlive
       (drop 3)           ;; remove useless stuff at top
       (transform-enlive site-source-dir available-files css)
       html/emit*         ;; turn back into html
       (apply str)))


(defn process-html-file! [site-source-dir target-dir file-name
                          available-files extra-css]
  (->> file-name
       (str site-source-dir "/")
       slurp
       (process-org-html site-source-dir available-files extra-css)
       (spit (str target-dir "/" file-name))))


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
        org-files (available-org-files site-source-dir)]
    (ensure-target-dir-exists! target-dir)
    (stage-site-image-files! site-source-dir target-dir)
    (doseq [f org-files]
      (process-html-file! site-source-dir
                          target-dir
                          (str f ".html")
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


;; Example, current workflow.
#_(let [home (env :home)
        remote-host "zerolib.com"
        site-source-dir (str home "/Dropbox/org/sites/" remote-host)
        target-dir "/tmp/organa-tmp2"
        key-location (str home "/.ssh/do_id_rsa")]
    (generate-static-site remote-host
                          site-source-dir
                          target-dir)
    (sh "open file://"  target-dir "/index.html")
    #_(sync-tmp-files-to-remote! key-location
                                 target-dir
                                 remote-host
                                 (str "/www/" remote-host))
    #_(sh "open http://" remote-host))
