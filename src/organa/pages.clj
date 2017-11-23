(ns organa.pages
  (:require [organa.html :refer :all]))


(defn blog-body []
  (div {:id "content"} ["Hi, I'm a blog!"])
  (div {:id "blogposts"} []))


(defn home-body []
  (div {:id "content"}
       [(h1 "John Jacobsen")
        (div {:class "figure"}
             [(a {:href "view-from-skylab-cropped-large.png"}
                  [(img {:src "view-from-skylab-cropped.jpg"
                         :width 600} [])])
              (p {:class "figure-number"
                  :style "margin-top:3px"}
                 ["Clean Air facility as seen from Skylab, South Pole, 1998"])])
        (p "Hi, I'm an artist, software engineer and former physicist living in
               Chicago, Illinois, USA.")
        (p ["As an artist, I'm primarily interested in figurative oil
            painting and drawing.  You can see samples of my recent work on "
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
                                 "Instagram")
                              " (recent work / in progress)"])
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
        (p ["Aside from the above links, this site is primarily
            here to present to you the following blog posts.  Many of
            these were "
            (a {:href "southpole-blog.html"}
               ["written at the Geographic South Pole"])
            " between 2007 and 2011."])
        (div {:class "hspace"} [])
        (div {:id "blogposts"} [])]))
