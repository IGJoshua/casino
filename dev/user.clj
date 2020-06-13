(ns user
  (:require
   [casino.commands :as c]
   [casino.events :as e]
   [casino.house :as h]
   [casino.middleware :as mdw :refer [make-logger handler->middleware]]
   [casino.slot-machine :as slots]
   [casino.state :refer [*messaging* *connection*]]
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [discljord.connections :as d.c]
   [discljord.messaging :as d.m]
   [taoensso.timbre :as log]))

(def token (str/trim (slurp (io/resource "token-canary.txt"))))
(def owner (str/trim (slurp (io/resource "owner.txt"))))

(defonce connection-chan (atom nil))
(defonce messaging-chan (atom nil))

(defn stop-bot
  "Triggers a disconnect in the bot."
  []
  (when @messaging-chan
    (d.m/stop-connection! @messaging-chan))
  (when @connection-chan
    (d.c/disconnect-bot! @connection-chan)))

(defn disconnect-bot-command
  "Command handler which calls [[stop-bot]]"
  [_ _ _]
  (stop-bot))

(def debug-commands
  "Additional commands to add to the bot when running the canary version."
  [[#"ping" #'c/pong]
   [#"disconnect" #'disconnect-bot-command]])

(def debug-command-middleware
  "Prepares messages for debug command handling.

  This middleware should only be used on streams for :message-create events."
  (mdw/make-transducer
   (comp (filter (fn [[_ event-data]]
                   (= owner (:id (:author event-data)))))
         (filter (fn [[_ event-data]]
                   (str/starts-with? (:content event-data) "d!")))
         (map (mdw/data-transform
               (fn [{:keys [content] :as event}]
                 (assoc event :content (subs content 2))))))))

(defn save-connections
  "Event handler which ignores input and sets the connection atoms."
  [_ _]
  (reset! connection-chan *connection*)
  (reset! messaging-chan *messaging*))

(def extra-handlers
  "Additional handlers for events to assist debugging."
  {:ready [#'save-connections]
   :message-create [(debug-command-middleware (e/make-run-commands #'debug-commands))]})

(def event->logging-level
  "Map from event types to the logging level it should be logged at."
  {:message-create :debug
   :ready :info})

(defn logging-filter
  [event-type event-data]
  (or (event->logging-level event-type)
      :debug))

(def middleware
  "Middleware to run over the event handler for debugging at the repl."
  (comp (make-logger #'logging-filter)
        (handler->middleware
         (h/make-handler #'extra-handlers))))

(def extra-intents
  "Additional intents required for the middleware."
  #{})

(defn run-bot
  "Starts the bot with the canary token on a dedicated thread."
  []
  (a/thread (h/run
              token
              (middleware (h/make-handler #'h/handlers))
              (into h/intents extra-intents))))

(defn discljord-logging-level!
  "Sets the logging level for discljord."
  [level]
  (log/merge-config! {:ns-blacklist ["discljord.*"]
                      :appenders {:discljord (merge (log/println-appender)
                                                    {:ns-whitelist ["discljord.*"]
                                                     :min-level level})}}))

(defonce set-level (discljord-logging-level! :warn))

(defn average-payout
  "Determines the average payout of a given slot `machine` over a number of `trials`."
  [machine trials]
  (loop [bet 0
         won 0
         trials trials]
    (if (pos? trials)
      (let [cur-bet (+ (::slots/min-bet machine)
                       (rand-int (- (inc (::slots/max-bet machine))
                                    (::slots/min-bet machine))))
            [_ payout] (slots/play-slots machine cur-bet)]
        (recur (+ bet cur-bet) (+ won payout)
               (dec trials)))
      (double (/ won bet)))))

(defn payout-seq
  "Returns a lazy sequence of payouts for a given bet on the machine."
  [machine bet]
  (map second (repeatedly #(slots/play-slots machine bet))))
