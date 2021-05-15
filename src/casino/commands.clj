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
    (when-let [item (first clauses)]
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

(defn- make-spin-str
  [machine spin]
  (apply str
         (interpose "\n"
                    (map (fn [row]
                           (apply str (map (::display machine) (map ::slots/name row))))
                         (slots/rows spin)))))

(defn make-spin-embed
  "Makes an embed to display the results of a spin."
  [machine spin bet winnings]
  {:title (::name machine)
   :type "rich"
   :color (::color machine)
   :fields [{:name "Results"
             :value (make-spin-str machine spin)}
            {:name "Winnings"
             :value (str
                     "You bet $" bet "\n"
                     (if (zero? winnings)
                       "You lost!"
                       (str "You won $" winnings "!")))}]
   :footer {:text (::footer machine)}})

(def base-machine
  "Basic machine to play with while working on the bot."
  (assoc (slots/make-machine (map (partial apply slots/make-symbol)
                                  '((:hundred 100) (:hundred 100)
                                    (:eight-ball 88)
                                    (:super-seven 77) (:super-seven 77)
                                    (:melon 7) (:melon 7) (:melon 7)
                                    (:cherry 5) (:cherry 5) (:cherry 5)
                                    (:bell 2) (:bell 2) (:bell 2)
                                    (:checkmark 1) (:checkmark 1) (:checkmark 1)))
                             3 5
                             25 150)
         ::color 0x00FF00
         ::display {:hundred ":100:"
                    :eight-ball ":8ball:"
                    :super-seven ":seven:"
                    :melon ":watermelon:"
                    :cherry ":cherries:"
                    :bell ":bell:"
                    :checkmark ":ballot_box_with_check:"}
         ::name "Base Machine"
         ::footer "BOTTOM TEXT"))

(defn play-slots
  "Plays a game of slots."
  [[_ bet :as msg] event-type event-data]
  (let [bet (and (vector? msg)
                 (Long. bet))
        bet (or bet 75)
        [spin winnings] (slots/play-slots base-machine bet)]
    (m/create-message! *messaging* (:channel-id event-data)
                       :embed (make-spin-embed base-machine spin bet winnings))))
