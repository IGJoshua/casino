(ns casino.middleware
  "Provides middleware for debugging purposes on the event stream.

  Middleware functions are event handlers which can perform arbitrary
  transformations on the events before passing them to the original handler, and
  may opt to not pass an event to the original handler at all. "
  (:require
   [taoensso.timbre :as log]))

(defn make-middleware
  "Takes a function and returns a middleware function which can be called on a handler.

  Middleware functions take three arguments, the original handler function, the
  event type, and the event data. They may take any actions, but their primary
  function is to call the original handler function with the event-type and the
  event-data."
  [middleware]
  (fn [handler]
    (fn [event-type event-data]
      (middleware handler event-type event-data))))

(defn handler->middleware
  "Takes a handler function and creates a middleware which concats the handlers.

  The events in the handler function passed are always run before the ones that
  are given to the middleware when it is applied."
  [handler]
  (make-middleware
   (fn [hnd event-type event-data]
     (handler event-type event-data)
     (hnd event-type event-data))))

(defn make-logger
  "Takes a predicate and if it returns true, logs the event before passing it on.

  The predicate must take the event-type and the event-data, and return a truthy
  value if it should log. If the value is a valid level at which to log, that
  logging level will be used."
  [filter]
  (make-middleware
   (fn [handler event-type event-data]
     (when-let [logging-level (filter event-type event-data)]
       (if (#{:trace :debug :info :warn :error :fatal} logging-level)
         (log/log logging-level event-type event-data)
         (log/debug event-type event-data)))
     (handler event-type event-data))))

(defn make-filter
  "Makes middleware that only calls the handler if `pred` returns truthy.

  `pred` is a predicate expected to take the event-type and event-data."
  [pred]
  (make-middleware
   (fn [handler event-type event-data]
     (when (pred event-type event-data)
       (handler event-type event-data)))))

(defn data-transform
  "Makes a transform function for use with [[make-transform]] that operates on event-data.

  The resulting function is from a vector of event-type and event-data to a
  vector of event-type and event-data. The event-type is passed through without
  change, and event-data is transformed by `f`."
  [f]
  (fn [[event-type event-data]]
    [event-type (f event-data)]))

(defn make-transform
  "Makes a middleware which runs `f` over events before passing to the handler.

  `f` is a function of a vector of event-type and event-data to a vector of
  event-type and event-data which are then passed to the handler."
  [f]
  (make-middleware
   (fn [handler event-type event-data]
     (let [[event-type event-data] (f [event-type event-data])]
       (handler event-type event-data)))))

(defn make-transducer
  "Makes a middleware which takes a transducer and runs it over event-data."
  [xf]
  (let [reduced (volatile! false)
        reducer (fn [send-event [event-type event-data]]
                  (send-event event-type event-data))
        reducer (xf reducer)]
    (make-middleware
     (fn [handler event-type event-data]
       (when-not @reduced
         (let [res (reducer handler [event-type event-data])]
           (when (reduced? res)
             (vreset! reduced true))
           res))))))

(def ignore-bot-messages
  "Middleware which won't call the handler if the event is from a bot."
  (make-filter
   (fn [event-type event-data]
     (if (#{:message-create :message-update} event-type)
       (when-not (:bot event-data)
         true)
       true))))
