(ns casino.test-utils
  (:require
   [clojure.pprint :as pp]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :as t]))

(defn test-specced-fns
  "Runs auto-generated tests from a function's spec."
  [syms]
  (let [check-result (stest/check syms)
        results (map stest/abbrev-result check-result)]
    (t/is (= 0 (count (filter :failure results)))
          (with-out-str (pp/pprint results)))))
