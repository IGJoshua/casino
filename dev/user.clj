(ns user
  (:require
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [casino.house :as h]))

(def token (str/trim (slurp (io/resource "token-canary.txt"))))

(defn run-bot
  "Starts the bot with the canary token on a dedicated thread."
  []
  (a/thread (h/run token)))
