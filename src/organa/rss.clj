(ns organa.rss
  (:require [clj-rss.core :as rss]
            [clojure.walk :as walk]
            [net.cgrand.enlive-html :as html]
            [organa.config :refer [config]]
            [organa.html :as h]))

;; FIXME:
(def target-dir (:target-dir config))
;; FIXME:
(def remote-host (:remote-host config))

(defn ^:private html-for-rss
  "
  Remove JavaScript and CSS bits from Org-generated HTML for RSS feed.
  "
  [parsed-html]
  (walk/prewalk
   (fn [x]
     (if (and (map? x)
              (#{"text/css" "text/javascript"} (get-in x [:attrs :type])))
       (dissoc (into {} x) :content)
       x))
   parsed-html))

(defn make-rss-feeds
  ;; FIXME: reduce arity
  ([topic rss-file-name org-files]
   (make-rss-feeds rss-file-name
                   (filter (comp (partial some #{topic})
                                 :tags)
                           org-files)))
  ([rss-file-name org-files]
   (let [rss-file-path (str target-dir "/" rss-file-name)
         posts-for-feed (->> org-files
                             (remove :static?)
                             (remove :draft?)
                             (take 20))
         feed-items (for [f posts-for-feed
                          :let [file-name (:file-name f)
                                link-path (format "http://%s/%s.html"
                                                  remote-host
                                                  file-name)]]
                      {:title (:title f)
                       :link link-path
                       :pubDate (.toDate ^org.joda.time.DateTime (:date f))
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

(defn rss-links []
  (h/p ["Subscribe: "
        (h/a {:href "feed.xml"
              :class "rss"}
             ["RSS feed ... all topics"])
        " ... or "
        (h/a {:href "feed.clojure.xml"
              :class "rss"}
             ["Clojure only"])
        " / "
        (h/a {:href "feed.lisp.xml"
              :class "rss"}
             ["Lisp only"])]))
