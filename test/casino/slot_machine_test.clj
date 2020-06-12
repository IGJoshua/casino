(ns casino.slot-machine-test
  (:require
   [casino.slot-machine :as sut]
   [casino.test-utils :refer [test-specced-fns]]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :as t]
   [clojure.test.check :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

(t/deftest constructing-values
  (t/testing "basic make-thing functions"
    (test-specced-fns
     `(sut/make-symbol
       sut/make-machine))))
