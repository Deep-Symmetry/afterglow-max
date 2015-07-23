(ns afterglow.max.core
  (:require [afterglow.core]
   [taoensso.timbre :as timbre])
  (:import (com.cycling74.max MaxObject DataTypes)))

(defn max-console-appender
  "Returns an appender which writes to the Max console."
  []
  {:enabled?   true
   :async?     false
   :min-level  nil
   ;; Limit output to three every 30ms, and no more than ten every 30 seconds.
   :rate-limit [[3 30] [10 30000]]
   :output-fn  :inherit
   :fn
   (fn [data]
     (let [{:keys [output-fn]} data
           output-str (output-fn data)]
       (try
         (if (:error? data)
           (MaxObject/error output-str)
           (MaxObject/post output-str))
         (catch Throwable _))))})

(defn- init-internal
  "Performs the actual initialization, protected by the delay below
  to insure it happens only once."
  []
  (afterglow.core/init-logging {:max (max-console-appender)})
  (try
    (require 'afterglow-max-init)
    (catch Throwable t
      (timbre/error "Problem loading afterglow-max-init.clj" t))))

(defonce ^{:private true
           :doc "Used to ensure initialization takes place exactly once."}
  initialized (delay (init-internal)))

(defn init
  "Makes sure the Afterglow environment has been set up for
  use with Max, including evaluating any initialization forms
  in afterglow_max_init.clj, and directing log output to the
  Max console."
  []
  @initialized)
