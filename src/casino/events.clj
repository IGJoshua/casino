(ns casino.events
  "Event handlers which respond to individual event types sent by discord."
  (:require
   [casino.state :refer [state]]
   [discljord.messaging :as m]))

(defn store-user
  [ready-event {user :user}]
  (swap! state assoc :user user))
