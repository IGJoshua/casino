(ns user
  (:require
   [casino.house :as h]
   [casino.middleware :refer [make-logging-middleware handler->middleware]]
   [casino.state :refer [*messaging* *connection*]]
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [discljord.connections :as d.c]
   [discljord.messaging :as d.m]))

(def token (str/trim (slurp (io/resource "token-canary.txt"))))

(defonce connection-chan (atom nil))
(defonce messaging-chan (atom nil))

(def middleware
  "Middleware to run over the event handler for debugging at the repl."
  (comp (make-logging-middleware (constantly :info))
        (handler->middleware
         (fn [event-type event-data]
           (when (= event-type :ready)
             (reset! connection-chan *connection*)
             (reset! messaging-chan *messaging*))))))

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

(defn stop-bot
  "Triggers a disconnect in the bot."
  []
  (when @messaging-chan
    (d.m/stop-connection! @messaging-chan))
  (when @connection-chan
    (d.c/disconnect-bot! @connection-chan)))
