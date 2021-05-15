(ns casino.house
  "A bot for playing interactive casino games."
  (:require
   [casino.commands :as c]
   [casino.events :as e]
   [casino.middleware :as m]
   [casino.state :refer [*messaging* *connection*]]
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [discljord.connections :as conn]
   [discljord.events :refer [message-pump!]]
   [discljord.messaging :as msg])
  (:gen-class))

(defn make-handler
  "Creates a function which calls each handler for a given event.

  Takes a map from keywords of events to vectors of handler functions which take
  the event-type received and the event data, and runs them in sequence,
  ignoring return results."
  [handlers]
  (fn [event-type event-data]
    (doseq [f (handlers event-type)]
      (f event-type event-data))))

(def commands
  "Standard commands for normal bot behavior"
  [[#"slots\s+(\d+)" #'c/play-slots]
   [#"slots" #'c/play-slots]])

(def command-middleware
  "Prevents bot messages and unrelated messages from being processed."
  (m/make-transducer
   (comp m/ignore-bot-messages
         (filter (fn [[_ event-data]]
                   (str/starts-with? (:content event-data) "!")))
         (map (m/data-transform
               (fn [{:keys [content] :as event}]
                 (assoc event :content (subs content 1))))))))

(def handlers
  "Map from discord event types to vars of functions to handle those events.

  This is the default set of event handlers which are required for the bot."
  {:ready [#'e/store-user]
   :message-create [(command-middleware (e/make-run-commands #'commands))]})

(def intents
  "Set of all intents which are required for the bot to function properly.

  This is closely tied to [[handlers]] because the specific handlers used
  determines which intents are needed."
  #{:guild-messages})

(defn run
  "Starts a bot using the given `token`."
  ([token handler] (run token handler #{}))
  ([token handler intents]
   (let [events-chan (a/chan 10000)
         messaging-chan (msg/start-connection! token)
         connection-chan (conn/connect-bot! token events-chan
                                            :intents intents)]
     (binding [*messaging* messaging-chan
               *connection* connection-chan]
       (message-pump! events-chan handler)))))

(defn -main
  "Starts a bot with the main token, shuts down agents once it stops."
  [& args]
  (run
    (str/trim (slurp (io/resource "token.txt")))
    (make-handler #'handlers)
    intents)
  (shutdown-agents))
