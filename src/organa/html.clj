(ns organa.html
  (:require [net.cgrand.enlive-html :as html]))


(defmacro deftag [tagname]
  `(defn ~tagname
     ([~'content] (~tagname {} ~'content))
     ([~'attrs ~'content]
      {:tag ~(keyword tagname)
       :attrs ~'attrs
       :content ~'content})))


(deftag a)
(deftag div)
(deftag h1)
(deftag h2)
(deftag img)
(deftag li)
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
(defn br [] {:tag :br})
(defn hr [] {:tag :hr})


(defn parse-org-html [site-source-dir basename]
  (-> (format "%s/%s.html" site-source-dir basename)
      slurp
      html/html-snippet))
