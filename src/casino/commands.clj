(ns casino.commands
  "Functions for creating and evaluating commands."
  (:require
   [casino.state :refer [*messaging*]]
   [casino.slot-machine :as slots]
   [clojure.pprint :as pp]
   [discljord.messaging :as m]))

(defn eval-regex-commands
  "Evaluates a command structure given a string and extra args for the handlers.

  The command structure consists of a vector of vectors, with each inner vector
  making up a \"clause\" in the structure, where the first item is a regex used
  to match that clause, and the second is a function expected to take the result
  of calling [[re-matches]] with the regex on the input string, as well as any
  additional arguments given via `args`.

  If an element is not a vector, then it will be called unconditionally with the
  full string, and any additional arguments. No further clauses will be tested."
  {:arglists '([[regex+fn*] s & args]
               [[regex+fn* fallthrough] s & args])}
  [commands s & args]
  (loop [clauses (seq commands)]
    (let [item (first clauses)]
      (if (vector? item)
        (let [[re f] item]
          (if-let [match (re-matches re s)]
            (apply f match args)
            (recur (rest clauses))))
        (apply item s args)))))

(defn pong
  "Test command that sends \"pong\" when a message is recieved."
  [pinging-msg event-type event-data]
  (m/create-message! *messaging* (:channel-id event-data) :content "pong!"))

(def base-machine
  "Basic machine to play with while working on the bot."
  (slots/make-machine (map (partial apply slots/make-symbol)
                           '((:seven 70) (:seven 70) (:super-seven 777) (:eight-ball 88)
                             (:eight-ball 88)
                             (:hundred 1000)
                             (:cherry 15) (:cherry 15) (:cherry 15)
                             (:bell 2) (:bell 2) (:bell 2) (:bell 2)))
                      3 5
                      25 150))

(defn play-slots
  "Plays a game of slots."
  [slots-msg event-type event-data]
  (m/create-message! *messaging* (:channel-id event-data)
                     :content (with-out-str (pp/pprint (slots/play-slots base-machine 75)))))
