(ns watchtower.test.core
  (:use
    watchtower.core
    clojure.test)
  (:require
    [clojure.java.io :as io]))

;(deftest replace-me ;; FIXME: write
;  (is false "No tests have been written."))

(deftest extensions-test
  (testing "single extension"
    (let [f #((extensions :clj) (io/file %))]
      (is (f "foo.clj"))
      (is (not (f "fooclj")))
      (is (not (f "foo.txt")))
      (is (not (f "footxt")))
      ))

  (testing "multi extensions"
    (let [f #((extensions :clj :txt) (io/file %))]
      (is (f "foo.clj"))
      (is (not (f "fooclj")))
      (is (f "foo.txt"))
      (is (not (f "footxt")))))

  (testing "wildcard"
    (let [f #((extensions :*) (io/file %))]
      (is (f "foo.clj"))
      (is (f "fooclj"))
      (is (f "foo.txt"))
      (is (f "footxt"))))

  (testing "wildcard and extension"
    (let [f #((extensions :* :clj) (io/file %))]
      (is (f "foo.clj"))
      (is (f "fooclj"))
      (is (f "foo.txt"))
      (is (f "footxt"))))

  ;(testing "doubled extensions"
  ;  (let [f #((extensions :* :html.clj) (io/file %))]
  ;    (is (f "foo.html.clj"))
  ;    (is (not (f "foohtml.clj")))
  ;    (is (not (f "foo.clj")))
  ;    (is (not (f "foo.html")))))
  )
