(ns organa.pages
  (:require [organa.html :as h]))

(defn blog-body []
  (h/div {:id "content"} ["Hi, I'm a blog!"])
  (h/div {:id "blogposts"} []))

(defn home-body []
  (h/div
   {:id "content"}
   [(h/h1 "John Jacobsen ...")
    (h/div {:class "figure"}
           [(h/a {:href "view-from-skylab-cropped-large.png"
                  :class "img100"}
                 [(h/img {:src "view-from-skylab-cropped.jpg"} [])])
            (h/p {:class "figure-number"
                  :style "margin-top:3px"}
                 [(str "Clean Air facility as seen from Skylab, "
                       "Amundsen-Scott South Pole Station, "
                       "Antarctica, 1998")])])
    (h/p [(str "... is an artist, software engineer and former "
               "physicist living in Chicago, Illinois, USA. ")
          (h/a {:href "contact.html"} "Contact me.")])
    (h/div
     [(h/table
       {:class "frontpage-2col"}
       [(h/tbody
         [(h/tr [(h/td [(h/p
                         ["As an " (h/strong ["engineer"])
                          ", I'm interested in helping companies "
                          "and organizations create excellent "
                          "(functional, reliable, maintainable) "
                          "software, and teams to build the same. "
                          (h/a {:href "tech.html"} "Read more here")
                          "."])
                        (h/p [(h/a {:href "https://github.com/eigenhombre"}
                                   "GitHub")
                              " / "
                              (h/a {:href "https://twitter.com/eigenhombre"}
                                   "Twitter")
                              " / "
                              (h/a {:href
                                    (str "http://stackoverflow.com"
                                         "/users/611752/johnj")}
                                   "StackOverflow")
                              " / "
                              (h/a {:href
                                    "https://www.linkedin.com/in/eigenhombre"}
                                   "LinkedIn")
                              " / "
                              (h/a {:href "static/jacobsen-resume.pdf"}
                                   "Resume")])
                        ])
                 (h/td {:class "colspacer"} [])
                 (h/td
                  [(h/p
                    ["As an " (h/strong ["artist"])
                     ", I'm primarily interested in figurative oil "
                     "painting and drawing.  You can see samples of "
                     "my recent work on "
                     (h/a {:href "http://instagram.com/eigenhombre"}
                          "Instagram")
                     " or "
                     (h/a {:href "http://toomanysketchbooks.tumblr.com"}
                          "Tumblr")
                     ".  See also "
                     (h/a {:href "bio.html"}
                          "bio")
                     " and "
                     (h/a {:href "exhibitions.html"}
                          "exhibitions.")])])])])])])
    (h/p ["Aside from the above links, this site is primarily
            here to present to you the following blog posts.  Many of
            these were "
          (h/a {:href "southpole.html"}
               ["written at the Geographic South Pole"])
          " between 2007 and 2011."])
    (h/div {:id "blogposts"} [])]))
