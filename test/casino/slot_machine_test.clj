(ns casino.slot-machine-test
  (:require
   [casino.slot-machine :as sut]
   [casino.test-utils :refer [test-specced-fns]]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :as t]
   [clojure.test.check :as tc]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]))

(alias 'stc 'clojure.spec.test.check)

(t/deftest spec-tests
  (t/testing "constructing values"
    (t/testing "basic make-thing functions"
      (test-specced-fns
       `(sut/make-symbol
         sut/make-machine)
       {::stc/opts {:num-tests 100}})))
  (t/testing "step functions"
    (test-specced-fns
     `(sut/spin
       sut/rows
       sut/in-bounds?
       sut/diagonals
       sut/runs)
     {::stc/opts {:num-tests 10}})))

(defspec get-from-coords 20
  (prop/for-all
   [bounds (gen/fmap (fn [[x y]] [(inc x) (inc y)])
                     (gen/tuple gen/nat gen/nat))
    x gen/nat
    y gen/nat]
   (let [res (sut/in-bounds? bounds [x y])]
     (if (and (< x (first bounds))
              (< y (second bounds)))
       res
       (not res)))))

(defspec fetch-gets-symbol 20
  (prop/for-all
   [spin (s/gen ::sut/spin-state)
    x gen/nat
    y gen/nat]
   (s/valid? ::sut/symbol
             (sut/symbol-at-coord spin
                                  [(rem x (count spin))
                                   (rem y (count (first spin)))]))))
