(ns organa.mirror
  (:require [cheshire.core :as json]
            [clojure.java.shell :as sh]
            [net.cgrand.enlive-html :as html]
            [java-time :as jt]
            [organa.html :as h]))

(defn read-temp-posts-file []
  (slurp "/tmp/posts.json"))

(defn parse-file [txt]
  (json/parse-string txt true))

(defn epoch-secs->local-date-time [n]
  (jt/plus (jt/local-date-time 1970)
           (jt/seconds n)))

(defn relevant-data [m]
  (let [[caption-text best-image timestamp]
        ((juxt (comp :text :caption)
               (comp last (partial sort-by :width) :candidates :image_versions2)
               :taken_at)
         m)]
    (assoc best-image
           :caption caption-text
           :timestamp (epoch-secs->local-date-time timestamp))))

(defn abbrev [s] (->> s
                      (take 10)
                      (apply str)))

(defn post->enl [{:keys [url]}]
  (h/div
   [(h/div {:style "width:300px;height:300px;overflow:hidden;background-size:cover;"}
           [(h/img {:width 300
                    :src url}
                   [])])]))

(defn ->html [posts]
  (let [enl-posts (map post->enl posts)
        rows (partition-all 3 enl-posts)
        total-enlive (h/div [(h/table
                              (for [r rows]
                                (h/tr (for [i r]
                                        (h/td [i])))))])]
    (->> total-enlive
         html/emit*
         (apply str))))

(defn preview-file [fname]
  (sh/sh "open" fname))

(comment
  (let [html
        (->> (read-temp-posts-file)
             parse-file
             (map relevant-data)
             ->html)]
    (spit "/tmp/posts.html" html)
    (preview-file "/tmp/posts.html")))
