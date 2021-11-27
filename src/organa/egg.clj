(ns organa.egg
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))

(defn ^:private whodonit []
  (rand-nth ["Produced by little elves with hand tools"
             "Make by perky little white bunnies"
             "Produced in high-energy, heavy ion impacts"
             "Forged in the fires of Odin"
             "Seen in the Tannhauser gates"
             "Swept under your carpet"
             "Crashed into brick walls at high speed"
             "Atomized into little silver flakes"]))

(def ^:private egg-fmt (slurp (io/resource "banner-format.txt")))

(defn easter-egg
  "
  Generate a (potentially random) \"easter egg\" to embed as a comment
  in blog post HTML.
  "
  []
  (let [who (whodonit)
        n (- 62 (count who))
        line3 (str "            ``` "
                   who
                   ", using "
                   (string/join (repeat n "`")))]
    (format egg-fmt line3)))
