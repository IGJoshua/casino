(ns casino.house
  (:require
   [casino.state :refer [*messaging* *connection*]]
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [discljord.connections :as c]
   [discljord.events :refer [message-pump!]]
   [discljord.messaging :as m])
  (:gen-class))

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
