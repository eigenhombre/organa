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


(defn transform-enlive [available-files css enl]
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
                           (concat
                            [{:tag :p
                              :content ["Available files:"]}]
                            (for [f available-files]
                              {:tag :p
                               :content [{:tag :a
                                          :attrs {:href (str f ".html")}
                                          :content [f]}]})
                            [(footer)]))))


(defn process-org-html [available-files css txt]
  (->> txt
       html/html-snippet  ;; convert to Enlive
       (drop 3)           ;; remove useless stuff at top
       (transform-enlive available-files css)
       html/emit*         ;; turn back into html
       (apply str)))


(defn sh [& cmds]
  (apply clojure.java.shell/sh
         (clojure.string/split (apply str cmds) #"\s+")))


;; Bash-y stuff ............................................
(defn ensure-target-dir-exists! [target-dir]
  (sh "mkdir -p " target-dir))


(defn stage-site-image-files! [local-site-dir target-dir]
  (doseq [f (->> local-site-dir
                 clojure.java.io/file
                 file-seq
                 (map #(.toString %))
                 (filter (partial re-find #"\.png|\.gif$")))]
    (sh "cp " f " " target-dir)))


(defn available-org-files [local-site-dir]
  (->> local-site-dir
       clojure.java.io/file
       file-seq
       (filter (comp #(.endsWith % ".org") str))
       (map #(.getName %))
       (map #(.substring % 0 (.lastIndexOf % ".")))))


(defn process-html-file! [local-site-dir target-dir file-name
                          available-files extra-css]
  (->> file-name
       (str local-site-dir "/")
       slurp
       (process-org-html available-files extra-css)
       (spit (str target-dir "/" file-name))))


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


(defn generate-static-site [remote-host
                            source-site-dir
                            target-dir]
  (let [css (->> "index.garden"
                 (str source-site-dir "/")
                 load-file
                 to-css)
        org-files (available-org-files source-site-dir)]
    (ensure-target-dir-exists! target-dir)
    (stage-site-image-files! source-site-dir target-dir)
    (doseq [f org-files]
      (process-html-file! source-site-dir
                          target-dir
                          (str f ".html")
                          org-files
                          css))))


;; Example, current workflow.
#_(let [home (env :home)
        remote-host "zerolib.com"
        source-site-dir (str home "/Dropbox/org/sites/" remote-host)
        target-dir "/tmp/organa-tmp"
        key-location (str home "/.ssh/do_id_rsa")]
    (generate-static-site remote-host
                          source-site-dir
                          target-dir)
    (sh "open file://"  target-dir "/index.html")
    #_(sync-tmp-files-to-remote! key-location
                                 target-dir
                                 remote-host
                                 (str "/www/" remote-host))
    #_(sh "open http://" remote-host))

