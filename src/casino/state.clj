(ns casino.state)

(defonce ^:dynamic *messaging* nil)
(defonce ^:dynamic *connection* nil)

(defonce state (atom {}))
