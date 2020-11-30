(ns organa.html
  (:require [net.cgrand.enlive-html :as html]
            [organa.profile :refer [with-profile-key]]))

(defmacro deftag
  ([fname tagname]
   `(defn ~fname
      ([~'content] (~fname {} ~'content))
      ([~'attrs ~'content]
       {:tag ~(keyword tagname)
        :attrs ~'attrs
        :content ~'content})))
  ([tagname_]
   `(deftag ~tagname_ ~tagname_)))

(deftag a)
(deftag div)
(deftag h1)
(deftag h2)
(deftag img)
(deftag li)
(deftag link)
(deftag p)
(deftag pre)
(deftag script)
(deftag span)
(deftag strong)
(deftag style)
(deftag table)
(deftag tbody)
(deftag thead)
(deftag tr)
(deftag td)
(deftag th)
(deftag em)
(deftag ul)
(deftag meta-tag meta)
(defn br [] {:tag :br})
(defn hr [] {:tag :hr})

(defn parse-org-html
  ([html-text]
   (with-profile-key :parse-org-html-1
     (html/html-snippet html-text)))
  ([source-dir basename]
   (with-profile-key :parse-org-html-2
     (-> (format "%s/%s.html" source-dir basename)
         slurp
         html/html-snippet))))
