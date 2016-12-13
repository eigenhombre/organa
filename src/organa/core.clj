(ns organa.core
  (:require [net.cgrand.enlive-html :as html]
            [environ.core :refer [env]]
            [garden.core :refer [css] :rename {css to-css}]))


;; Org / HTML manipulation .................................

(defn ^:private remove-newlines [m]
  (update-in m [:content] (partial remove (partial = "\n"))))


(defn ^:private year [] (+ 1900 (.getYear (java.util.Date.))))


(defn ^:private copyright-str [yr] (format "© %d John Jacobsen." yr))


(defn ^:private footer []
  {:tag :p
   :attrs {:class "footer"}
   :content [(copyright-str (year))]})


(defn remove-stuff [css enl]
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
           [:div#content] (html/append [(footer)])))


(defn process-org-html [css txt]
  (->> txt
       html/html-snippet  ;; convert to Enlive
       (drop 3)           ;; remove useless stuff at top
       (remove-stuff css) ;; remove other useless org stuff
       html/emit*         ;; turn back into html
       (apply str)))


;; CSS Stuff ...............................................

(defn sh [& cmds]
  (apply clojure.java.shell/sh
         (clojure.string/split (apply str cmds) #"\s+")))


;; Bash-y stuff ............................................
(defn ensure-tmp-site-dir-exists! [tmp-site-dir]
  (sh "mkdir -p " tmp-site-dir))


(defn stage-site-image-files! [local-site-dir tmp-site-dir]
  (doseq [f (->> local-site-dir
                 clojure.java.io/file
                 file-seq
                 (map #(.toString %))
                 (filter (partial re-find #"\.png|\.gif$")))]
    (sh "cp " f " " tmp-site-dir)))


(defn process-html-file! [local-site-dir tmp-site-dir file-name extra-css]
  (->> file-name
       (str local-site-dir "/")
       slurp
       (process-org-html extra-css)
       (spit (str tmp-site-dir "/" file-name))))


(defn sync-tmp-files-to-remote! [key-location
                                 tmp-site-dir
                                 remote-host
                                 remote-dir]
  ;; FIXME: Make less hack-y:
  (spit "/tmp/sync-script"
        (format "rsync --rsh 'ssh -i %s' -vurt %s/* root@%s:%s"
                key-location
                tmp-site-dir
                remote-host
                remote-dir))
  (sh "bash /tmp/sync-script"))


(comment
  ;; Example, current workflow.
  (let [home (env :home)
        remote-host "zerolib.com"
        local-site-dir (str home "/Dropbox/org/sites/" remote-host)
        tmp-site-dir "/tmp/organa-tmp"
        key-location (str home "/.ssh/do_id_rsa")
        css (->> "index.garden"
                 (str local-site-dir "/")
                 load-file
                 to-css)]
    (ensure-tmp-site-dir-exists! tmp-site-dir)
    (stage-site-image-files! local-site-dir tmp-site-dir)
    (process-html-file! local-site-dir tmp-site-dir "index.html" css)
    (sync-tmp-files-to-remote! key-location
                               tmp-site-dir
                               remote-host
                               (str "/www/" remote-host))
    (sh "open http://" remote-host)))
