(ns casino.slot-machine
  "Functions for playing with a slot machine."
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::value pos-int?)
(s/def ::name keyword?)
(s/def ::symbol (s/keys :req [::value ::name]))
(s/def ::symbols (s/coll-of ::symbol :kind set?))

(defn make-symbol
  [name value]
  {::value value
   ::name name})
(s/fdef make-symbol
  :args (s/cat :name ::name
               :value ::value)
  :ret ::symbol)

(s/def ::rows pos-int?)
(s/def ::columns pos-int?)
(s/def ::machine (s/keys :req [::symbols ::rows ::columns]))

(defn make-machine
  "Creates a slot machine.

  If no row or column size is provided, the default of 1 row, 3 columns is
  used."
  ([symbols]
   (make-machine symbols 1 3))
  ([symbols rows columns]
   {::symbols (set symbols)
    ::rows rows
    ::columns columns}))
(s/fdef make-machine
  :args (s/cat :symbols (s/coll-of ::symbol)
               :size (s/? (s/cat :rows ::rows
                                 :columns ::columns)))
  :fn (fn [{:keys [args ret]}]
        (if (:size args)
          (and (= (:rows (:size args))
                  (::rows ret))
               (= (:columns (:size args))
                  (::columns ret)))
          true))
  :ret ::machine)
