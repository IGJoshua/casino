(ns casino.house
  "A bot for playing interactive casino games."
  (:require
   [casino.state :refer [*messaging* *connection*]]
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [discljord.connections :as c]
   [discljord.events :refer [message-pump!]]
   [discljord.messaging :as m])
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

(def handlers
  "Map from discord event types to vars of functions to handle those events.

  This is the default set of event handlers which are required for the bot."
  {})

(def intents
  "Set of all intents which are required for the bot to function properly.

  This is closely tied to [[handlers]] because the specific handlers used
  determines which intents are needed."
  #{})

(defn run
  "Starts a bot using the given `token`."
  ([token handler] (run token handler #{}))
  ([token handler intents]
   (let [events-chan (a/chan 10000)
         messaging-chan (m/start-connection! token)
         connection-chan (c/connect-bot! token events-chan
                                         :intents intents)]
     (binding [*messaging* messaging-chan
               *connection* connection-chan]
       (message-pump! events-chan handler)))))

(defn -main
  "Starts a bot with the main token, shuts down agents once it stops."
  [& args]
  (run
    (str/trim (slurp (io/resource "token.txt")))
    (fn [_ _]))
  (shutdown-agents))
