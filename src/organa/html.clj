(ns organa.html)


(defmacro deftag [tagname]
  `(defn ~tagname
     ([~'content] (~tagname {} ~'content))
     ([~'attrs ~'content]
      {:tag ~(keyword tagname)
       :attrs ~'attrs
       :content ~'content})))


(deftag a)
(deftag div)
(deftag h2)
(deftag p)
(deftag span)
(deftag strong)
(deftag style)
(deftag em)
(defn br [] {:tag :br})
(defn hr [] {:tag :hr})
