(ns organa.home
  (:require [organa.html :refer :all]))


(defn home-body []
  (div {:id "content"}
       [(h1 "John Jacobsen")
        (p "Hi, I'm an artist and software engineer living in
               Chicago, Illinois, USA.")
        (p ["As an artist, I'm primarily interested in figurative oil
            painting and drawing.  You can see samples of my work on "
            (a {:href "http://instagram.com/eigenhombre"} "Instagram")
            " or "
            (a {:href "http://toomanysketchbooks.tumblr.com"} "Tumblr")
            "."])
        (p ["As an engineer, I'm interested in helping companies and
            organizations create excellent (functional, reliable,
            maintainable) software, and teams to build the same. "
            (a {:href "tech.html"} "Read more here") "."])
        (div
         [(table {:class "links"}
                 [(thead
                   [(tr [(th {:class "table"}
                             "Art Works")
                         (th {:class "table"}
                             "Engineering")])])
                  (tbody
                   [(tr [(td [(a {:href "http://instagram.com/eigenhombre"}
                                 "Instagram")])
                         (td [(a {:href "https://github.com/eigenhombre"}
                                 "GitHub")])])
                    (tr [(td [(a {:href "http://toomanysketchbooks.tumblr.com/"}
                                 "Tumblr")])
                         (td [(a {:href "https://twitter.com/eigenhombre"}
                                 "Twitter")])])
                    (tr [(td [(a {:href "http://johnj.com"}
                                 "Old Art Web site")])
                         (td [(a {:href "http://eigenhombre.com"}
                                 "Old Tech Web Site")])])])])])
        (p "Aside from the above links, this site is primarily
            here to present to you the following blog posts.  Many of
            these were written at the Geographic South Pole between
            2007 and 2011.")
        (div {:class "hspace"} [])
        (div {:id "blogposts"} [])]))
