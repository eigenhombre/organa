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
                        :class "fullwidth"} [])])
              (p {:class "figure-number"
                  :style "margin-top:3px"}
                 ["Clean Air facility as seen from Skylab, South Pole, 1998"])])
        (p "Hi, I'm an artist, software engineer and former physicist living in
               Chicago, Illinois, USA.")
        (div
         [(table
           {:class "frontpage-2col"}
           [(tbody
             [(tr [(td [(p ["As an " (strong ["engineer"])
                            ", I'm interested in helping companies "
                            "and organizations create excellent "
                            "(functional, reliable, maintainable) "
                            "software, and teams to build the same. "
                            (a {:href "tech.html"} "Read more here") "."])
                        (p [(a {:href "https://github.com/eigenhombre"}
                               "GitHub")
                            " / "
                            (a {:href "https://twitter.com/eigenhombre"}
                               "Twitter")
                            " / "
                            (a {:href
                                (str "http://stackoverflow.com"
                                     "/users/611752/johnj")}
                               "StackOverflow")
                            " / "
                            (a {:href
                                "https://www.linkedin.com/in/eigenhombre"}
                               "LinkedIn")
                            " / "
                            (a {:href "static/jacobsen-resume.pdf"}
                               "Resume")])
                        ])
                   (td {:class "colspacer"} [])
                   (td [(p ["As an " (strong ["artist"])
                            ", I'm primarily interested in figurative oil "
                            "painting and drawing.  You can see samples of "
                            "my recent work on "
                            (a {:href "http://instagram.com/eigenhombre"}
                               "Instagram")
                            " or "
                            (a {:href "http://toomanysketchbooks.tumblr.com"}
                               "Tumblr")
                            "."])])])])])])
        (p ["Aside from the above links, this site is primarily
            here to present to you the following blog posts.  Many of
            these were "
            (a {:href "southpole-blog.html"}
               ["written at the Geographic South Pole"])
            " between 2007 and 2011."])
        (div {:id "blogposts"} [])]))
