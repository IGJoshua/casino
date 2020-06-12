(ns casino.events
  "Event handlers which respond to individual event types sent by discord."
  (:require
   [casino.commands :as c]
   [casino.state :refer [state]]
   [discljord.messaging :as m]))

(defn store-user
  "Stores the user object for the bot in the [[state]] atom."
  [ready-event {user :user}]
  (swap! state assoc :user user))

(defn make-run-commands
  "Makes an event handler which evaluates the given commands.

  All commands are expected to take the event type and the full event-data as
  extra args.

  The event handler created expects the event-data to be a message object."
  [commands-or-var]
  (fn [event-type event-data]
    (c/eval-regex-commands (if (var? commands-or-var) @commands-or-var commands-or-var)
                           (:content event-data)
                           event-type event-data)))
