(ns casino.slot-machine
  "Functions for playing with a slot machine."
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test.check.generators :as gen]))

(s/def ::value pos-int?)
(s/def ::name keyword?)
(s/def ::symbol (s/keys :req [::value ::name]))
(s/def ::symbols (s/coll-of ::symbol))

(defn make-symbol
  [name value]
  {::value value
   ::name name})
(s/fdef make-symbol
  :args (s/cat :name ::name
               :value ::value)
  :ret ::symbol)

(s/def ::rows pos-int?)
(s/def ::columns (s/and pos-int?
                        #(>= % 3)))
(s/def ::min-bet nat-int?)
(s/def ::max-bet nat-int?)
(s/def ::machine (s/with-gen
                   (s/and (s/keys :req [::symbols ::rows ::columns
                                        ::min-bet ::max-bet])
                          #(> (count (::symbols %)) (::rows %))
                          #(> (::max-bet %) (::min-bet %)))
                   #(gen/let [cols (gen/fmap (partial + 3) gen/nat)
                              rows (gen/fmap inc gen/nat)
                              min-bet gen/nat
                              max-bet (gen/fmap (partial + min-bet) gen/nat)
                              machine (gen/return {::rows rows ::columns cols
                                                   ::min-bet min-bet ::max-bet max-bet})
                              symbols (gen/set (s/gen ::symbol)
                                               {:min-elements (inc (::rows machine))})]
                      (assoc machine ::symbols symbols))))

(defn make-machine
  "Creates a slot machine.

  If no row or column size is provided, the default of 1 row, 3 columns is
  used."
  ([symbols]
   (make-machine symbols 1 3))
  ([symbols rows columns]
   (make-machine symbols rows columns 1 5))
  ([symbols rows columns min-bet max-bet]
   (when (> (count symbols) rows)
     {::symbols symbols
      ::rows rows
      ::columns columns
      ::min-bet min-bet
      ::max-bet max-bet})))
(s/fdef make-machine
  :args (s/cat :symbols (s/coll-of ::symbol :min-count 2)
               :size (s/? (s/cat :rows ::rows
                                 :columns ::columns
                                 :bets (s/? (s/cat :min-bet ::min-bet
                                                   :max-bet ::max-bet)))))
  :fn (fn [{:keys [args ret]}]
        (if (:size args)
          (if (> (count (:symbols args)) (:rows (:size args)))
            (let [ret (second ret)]
              (and (= (:rows (:size args))
                      (::rows ret))
                   (= (:columns (:size args))
                      (::columns ret))))
            (nil? (second ret)))
          true))
  :ret (s/or :valid ::machine
             :nil nil?))

(s/def ::wheel (s/coll-of ::symbol :kind vector? :min-count 1))
(s/def ::spin-state (s/with-gen
                      (s/and (s/coll-of ::wheel :kind vector? :min-count 3)
                             #(apply = (map count %)))
                      #(gen/let [cols (gen/fmap (partial + 3) gen/nat)
                                 rows (gen/fmap inc gen/nat)]

                         (gen/vector (gen/vector-distinct (s/gen ::symbol)
                                                          {:num-elements rows})
                                     cols))))

(defn spin
  "Takes a slot machine and spins it, returning its row state."
  [machine]
  (let [wheels (map shuffle
                    (repeat (::columns machine) (::symbols machine)))]
    (mapv (comp vec #(take (::rows machine) %)) wheels)))
(s/fdef spin
  :args (s/cat :machine ::machine)
  :fn (fn [{:keys [args ret]}]
        (let [machine (:machine args)]
          (and (= (::columns machine) (count ret))
               (every? (comp (partial = (::rows machine)) count) ret))))
  :ret ::spin-state)

(s/def ::coord (s/tuple nat-int? nat-int?))

(defn in-bounds?
  "Tests if a point is within a certain bounds."
  [[max-x max-y] [test-x test-y]]
  (and (>= test-x 0)
       (>= test-y 0)
       (< test-x max-x)
       (< test-y max-y)))
(s/fdef in-bounds?
  :args (s/cat :bounds ::coord
               :coord ::coord))

(defn trace-diagonal
  "Returns a sequence of all coordinates on the diagonal given a direction."
  [bounds [start-x start-y] step-right?]
  (take-while (partial in-bounds? bounds)
              (map vector
                   (if step-right?
                     (iterate inc start-x)
                     (iterate dec start-x))
                   (iterate inc start-y))))
(s/fdef trace-diagonal
  :args (s/cat :bounds ::coord
               :start-coord ::coord
               :step-right? boolean?)
  :ret (s/coll-of (s/coll-of ::coord)))

(defn symbol-at-coord
  "Returns the symbol in the `spin` at the `coord`."
  [spin coord]
  (nth (nth spin (first coord)) (second coord)))
(s/fdef symbol-at-coord
  :args (s/cat :spin ::spin-state
               :coords ::coord)
  :ret ::symbol)

(defn diagonals
  "Returns a sequence of all diagonals in a spin state."
  [spin]
  (let [bounds [(count spin) (count (first spin))]
        spin-coords (vec (map-indexed
                          (fn [x v]
                            (vec (map-indexed
                                  (fn [y _]
                                    [x y])
                                  v)))
                          spin))
        starting-coords-right (concat
                               (reverse (first spin-coords))
                               (map first (rest spin-coords)))
        starting-coords-left (concat
                              (map first (butlast spin-coords))
                              (last spin-coords))
        diagonal-right (map #(trace-diagonal bounds % true) starting-coords-right)
        diagonal-left (map #(trace-diagonal bounds % false) starting-coords-left)]
    (map
     (fn [line]
       (map (partial symbol-at-coord spin) line))
     (concat diagonal-left diagonal-right))))
(s/fdef diagonals
  :args (s/cat :spin ::spin-state)
  :ret (s/coll-of (s/coll-of ::symbol)))

(defn rows
  "Returns a sequence of all rows in a spin state."
  [spin]
  (vec
   (for [n (range (count (first spin)))]
     (mapv #(nth % n) spin))))
(s/fdef rows
  :args (s/cat :spin ::spin-state)
  :fn (fn [{:keys [args ret]}]
        (and (= (count (first ret)) (count (:spin args)))
             (= (count ret) (count (first (:spin args))))))
  :ret (s/and (s/coll-of (s/coll-of ::symbol :kind vector? :min-count 3) :kind vector?)
              #(apply = (map count %))))

(defn runs
  "Returns a sequence of all runs in a spin state."
  [spin]
  (let [seqs (concat (diagonals spin) (rows spin) spin)]
    (->> seqs
         (map (fn [s]
                (->> s
                     (partition-by identity)
                     (filter #(>= (count %) 3))
                     flatten)))
         (remove (comp zero? count)))))
(s/fdef runs
  :args (s/cat :spin ::spin-state)
  :ret (s/coll-of (s/coll-of ::symbol)))

(defn score-for-run
  "Returns an integer score for a given run."
  [run]
  (* (transduce (map ::value) + 0 run)
     (count run)))
(s/fdef score-for-run
  :args (s/cat :run (s/coll-of ::symbol))
  :ret nat-int?)

(defn payout-for-bet
  "Returns the payout for the given machine given a bet and score."
  [machine bet score]
  (let [bet-ratio (+ 1 (/ (- bet (::min-bet machine))
                          (- (::max-bet machine)
                             (::min-bet machine))))]
    (int (* bet-ratio score))))
(s/fdef payout-for-bet
  :args (s/cat :machine ::machine
               :bet pos-int?
               :score pos-int?)
  :ret nat-int?)

(defn play-slots
  "Takes a machine and a bet and runs a spin, returning the payout."
  [machine bet]
  (let [spin (spin machine)
        runs (runs spin)
        score (transduce (map score-for-run) + 0 runs)]
    [spin (payout-for-bet machine bet score)]))
(s/fdef play-slots
  :args (s/cat :machine ::machine
               :bet pos-int?)
  :ret (s/cat :spin ::spin-state
              :payout nat-int?))
