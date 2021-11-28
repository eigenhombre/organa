(ns organa.html
  "
  Implementations of the parts of HTML needed by
  this blog engine.
  "
  (:require [net.cgrand.enlive-html :as html]))

(defmacro ^:private deftag
  ([fname tagname]
   `(defn ~fname
      ([~'content] (~fname {} ~'content))
      ([~'attrs ~'content]
       {:tag ~(keyword tagname)
        :attrs ~'attrs
        :content ~'content})))
  ([tagname]
   `(deftag ~tagname ~tagname)))

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

;; FIXME: Move to an org-mode-specific namespace.
(defn parse-org-html
  ([html-text]
   (html/html-snippet html-text))
  ([source-dir basename]
   (-> (format "%s/%s.html" source-dir basename)
       slurp
       html/html-snippet)))
