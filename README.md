# Organa

<img src="/organa.png" width="400">

![build](https://github.com/eigenhombre/organa/actions/workflows/build.yml/badge.svg)

# Introduction

This project implements a site generator for [my current
website](http://johnj.com/) using [Emacs Org Mode](http://orgmode.org/).

# Goals

- To provide a [Jekyll](https://jekyllrb.com/)-like (or better) workflow: edit markup files,
  have them automatically processed into HTML...
- using Org Mode as the markup language...
- supporting all of
  - my art- and image-heavy pages and image galleries;
  - my math- and code-heavy software posts;
  - my narrative-heavy South Pole blog posts.

# Strategy

In the past I've written a few (incomplete) Org Mode parsers
([here](https://github.com/eigenhombre/blorg) is a previous attempt), but HTML
is far easier to manipulate in Clojure using Hiccup or Enlive.  The current
code relies on one to export Org Mode to HTML first (with a few keystrokes in
Emacs).  The program parses the exported HTML, and modifies the parse tree as
needed to create cross links to other posts, etc.

# Workflow

If I were you, I wouldn't use this yet, other than to steal ideas from it,
since I haven't generalized it for multiple sites.  Since I often change
functionality and content at the same time, my current workflow is still
fairly REPL-driven:

- Evaluate the `core` namespace in the REPL to create the site and start the file watcher.
- Make edits to Org files in the source directory `site-source-dir`
  (or add new `.org` files there).
  - To change the CSS for the site, edit `index.garden`; source will
    be interpreted by the `garden` Clojure library and included in
    every page.
  - Static files that should be copied /verbatim/ into the /top level of the
    target site/ are added to `<site-source-dir>/static`. These are synced
    whenever `.org` files are updated.
  - To tag a post (for showing the post type in the navigation section of each
    page), add an empty section with the relevant tag(s), e.g.:

```
   * :mytag:othertag:
```

  - Directories of images in `<site-source-dir>/galleries` will be
    turned into static image galleries
- Export changed/added `.org` file(s) to HTML using `\C-c e hh`. This
  will cause the file watcher to reprocess the site.
- If needed, to restart the site-making code, re-evaluate the
  namespace after changing Clojure code /per se/.
- To "publish," use the commented-out forms at the bottom of the
  `core` namespace to `rsync` the code to the remote Web site.

# FAQ
## Why Org Mode?

I really like writing in [Org Mode](http://orgmode.org/) (a text editing /
outlining / To Do-list processing / scheduling / literate programming /
... mode for [Emacs](http://www.gnu.org/software/emacs/)).  The outliner gets
out of my way most of the time and lets me move ideas around while they are
being formed, and lets me hide the portions that I'm not focusing on at any
given time.  I can export to a fairly nice looking PDF document in a few
keystrokes.  I also use the literate programming and LaTeX / math support from
time to time.

## Why not /just/ Org Mode?

*I.e., why a Clojure app?*  I find the export tools available for Org Mode are
not quite powerful (or fast) enough for a large (> 100 posts) blog.  I got
pretty far trying to get the export features to suit, but not far enough --
generation of a large site took too long, and customization was too unweildy.
In general I much prefer developing software in [Clojure](http://clojure.org)
than in Emacs Lisp (though admittedly I'm less experienced with the latter).

## Why not Jekyll?

I used Jekyll for a few years and was somewhat satisfied by it.  But it
doesn't support Org Mode, and I am simply not that fond of Ruby and its
related ecosystems.  Also I have a number of customizations relating to
handling images that I'm unlikely to easily get working with Jekyll.

# Implementation

## Code / API documentation

[Codox](https://github.com/weavejester/codox) docs are
[here](https://raw.githack.com/eigenhombre/organa/master/docs/index.html).

# License

Copyright © 2016-2018, John Jacobsen. MIT License.

# Disclaimer

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
