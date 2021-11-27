(ns organa.fs-test
  (:require [clojure.java.shell :as shell]
            [clojure.test :refer [are deftest testing is]]
            [clojure.java.io :as io]
            [organa.fs :as fs]
            [clojure.string :as string])
  (:import [java.io File]))

(deftest with-tmp-dir-test
  (testing "It creates a file object"
    (is (instance? java.io.File
                   (fs/with-tmp-dir d
                     d))))
  (testing "It contains `organa` in the path"
    (is (string/includes? (fs/with-tmp-dir d
                            (:out (shell/sh "pwd" :dir d)))
                          "organa")))
  (testing "It cleans up after itself"
    (let [path (fs/with-tmp-dir d
                 (string/trim (:out (shell/sh "pwd" :dir d))))]
      (is (not
           (.exists (io/file path)))))))

(deftest path-test
  (are [args result]
    (testing (str args " => " (pr-str result))
      (is (= result (apply fs/path args))))

    [] ""
    nil ""
    ["a"] "a"
    ["a" "b"] "a/b"))

(deftest directory-test
  (testing "file / directory operations"
    (fs/with-tmp-dir d
      (testing "in a temporary directory"
        (let [test-file (fs/path d "f.txt")
              _ (spit test-file "some text")
              parent (fs/dirfile (io/file test-file))]
          (testing (str "the parent found by `dirfile` "
                        "has a single child, the test file we made")
            (is (= [(io/file test-file)]
                   (into [] (.listFiles ^File parent))))
            (testing "using `files-in-directory`, result is same"
              (is (= [(io/file test-file)]
                     (fs/files-in-directory (str parent)
                                            :as :file)))))
          (testing "`basename` returns original file name"
            (is (= "f.txt"
                   (fs/basename test-file))))
          (testing "`splitext` returns `basename` and extension"
            (is (= ["f" "txt"]
                   (fs/splitext test-file)))))))))
