(ns user
  (:require
   [casino.house :as h]
   [casino.repl.middleware :refer [make-logging-middleware]]
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def token (str/trim (slurp (io/resource "token-canary.txt"))))

(def middleware
  "Middleware to run over the event handler for debugging at the repl."
  (comp (make-logging-middleware (constantly :info))))

(defn run-bot
  "Starts the bot with the canary token on a dedicated thread."
  []
  (a/thread (h/run
              token
              (middleware (fn [_ _]))
              #{})))
