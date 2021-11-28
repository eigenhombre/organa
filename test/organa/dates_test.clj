(ns organa.dates-test
  (:require [clojure.test :refer [deftest testing is]]
            [java-time :as jt]
            [organa.dates :as d]
            [organa.fs :as fs]))

(deftest current-year-test
  (testing "I can get the current year, > 2020"
    (is (< 2020 (d/current-year)))))

(deftest date-for-org-file-test
  (testing "file creation dates"
    (fs/with-tmp-dir d
      (testing "An empty 'org' file"
        (let [fpath (fs/path d "empty.html")]
          (spit fpath "")
          (testing "creation date is found"
            (is (.startsWith (str (d/date-for-org-file d "empty"))
                             (str (d/current-year)))))))
      (testing "An 'org' file with a post date"
        (let [fpath (fs/path d "post.html")]
          (spit fpath "
<p class=\"date\">Date: 2008-02-18
")
          (testing "creation date is correct"
            (is (.startsWith (str (d/date-for-org-file d "post"))
                             "2008-02-18"))))))))
