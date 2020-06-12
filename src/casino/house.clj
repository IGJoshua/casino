(ns casino.house
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:gen-class))

(defn run
  "Starts a bot using the given `token`."
  [token])

(defn -main
  "Starts a bot with the main token, shuts down agents once it stops."
  [& args]
  (run (str/trim (slurp (io/resource "token.txt"))))
  (shutdown-agents))
