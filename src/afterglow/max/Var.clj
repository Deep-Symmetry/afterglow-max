(ns afterglow.max.Var
  "This implements a class that allows https://cycling74.com[Max] to
  set values in, and monitor changes to, an
  https://github.com/brunchboy/afterglow#afterglow[Afterglow] show
  variable, which may be a
  https://github.com/brunchboy/afterglow/blob/master/doc/parameters.adoc#variable-parameters[Variable
  Parameter], but may also have nothing to do with any
  https://github.com/brunchboy/afterglow/blob/master/doc/cues.adoc#the-cue-grid[cue
  grid] entries.

  Configured with the keyword that identifies the variable within the
  show, as you would use with <<show/set-variable!>>.

  The inlet and outlet allow the specified variable to be adjusted and
  monitored. The inlet responds to `int` or `float` messages by
  setting the variable, and responds to `bang` messages by sending the
  current variable value to outlet. If the variable has no value, the
  outlet is simply banged.

  The outlet reports the new value of the show variable whenever it
  changes, regardless of whether the change was caused by Max or
  outside of it."
  (:gen-class :extends com.cycling74.max.MaxObject
              :constructors {[String] []}
              :exposes-methods {declareIO parentDeclareIO
                                setInletAssist parentSetInletAssist
                                setOutletAssist parentSetOutletAssist
                                createInfoOutlet parentCreateInfoOutlet
                                getInlet parentGetInlet}
              :state state
              :init init
              :post-init post-init
              :main false
              :methods [])
  (:require [afterglow.max.core :as core]
            [afterglow.show :as show]
            [afterglow.show-context :refer [*show*]]
            [taoensso.timbre :as timbre])
  (:import (com.cycling74.max MaxObject)))

(defn -init
  "Called during the first phase of object construction with the
  keyword of the variable to bind to. Allocates object state."
  [k]
  (core/init)
  (when (nil? *show*)
    (throw (IllegalStateException. "Cannot create Var object: No default show has been established.")))
  (when (clojure.string/blank? k)
    (throw (IllegalStateException. "Cannot create Var object: No variable name provided.")))
  [[] (atom {:key (keyword k)})])

(defn send-numeric-value
  "Sends the value obtained from the variable to Max. If not a number,
  does nothing. If an integer, sends it as the argument of an `int`
  message. Otherwise, coerces to a float (since Max cannot handle
  ratios or other abstract numeric types) and sends it as the argument
  of a `float` message."
  [this v]
  (when (number? v)
    (.outlet this 0 (if (integer? v)
                      (int v)
                      (float v)))))

(defn -post-init
  "The post-init phase of the constructors tells Max about the inlets
  and outlets supported by this object, and registers our interest in
  variable value changes with Afterglow, setting up the function which
  sends reports on the outlet."
  [this k]
  (.parentDeclareIO this 1, 1)
  (.parentSetInletAssist this (into-array String ["int, float: Set; bang: Report current value"]))
  (.parentSetOutletAssist this (into-array String ["Output changed values"]))
  (.parentCreateInfoOutlet this false)

  ;; Set up the callback function for changes to variable value
  (let [f (fn [_ new-value] (send-numeric-value this new-value))]
    (swap! (.state this) assoc :f f)
    (show/add-variable-set-fn! (:key @(.state this)) f)))

(defn -bang
  "Respond to a bang message. Sends the current value of the variable
  to the outlet. If the variable has no value, the outlet is simply
  banged."
  [this]
  (let [k (:key @(.state this))
        v (show/get-variable k)]
    (if (some? v)
          (send-numeric-value this v)
          (.outletBang this 0))))

(defn -inlet-float
  "Respond to a float message, meaning the variable has been set to a
  floating point value."
  [this new-value]
  (show/set-variable! (:key @(.state this)) new-value))

(defn -inlet-int
  "Respond to an int message, meaning the variable has been set to an
  integer value."
  [this new-value]
  (show/set-variable! (:key @(.state this)) new-value))

(defn -notifyDeleted
  "The Max peer object has been deleted, so this instance is no longer
  going to be used. Unregister our variable change notification
  function, and allow this object to be garbage collected."
  [this]
  (let [{:keys [key f]} @(.state this)]
    (show/clear-variable-set-fn! key f)))
