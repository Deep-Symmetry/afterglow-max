(ns afterglow.max.Cue
  "This implements a class that allows https://cycling74.com[Max] from
  Cycling '74 to interact with
  https://github.com/brunchboy/afterglow#afterglow[Afterglow]
  https://github.com/brunchboy/afterglow/blob/master/doc/cues.adoc#the-cue-grid[cue
  grid] entries.

  Configured with the x and y coordinates of a cue in the default
  show's cue grid.

  The first inlet responds to `bang` in the same way a grid controller
  would: Starts the cue if it is not running, asks it to end if it is
  running, and kills it if it has already been asked to end but not
  yet finished doing so. It also responds to `start`, which will only
  ever start the cue, leaving it unaffected if it is already active,
  and `end` which ends it (gracefully the first time, forcefully
  thereafter), and `kill` which always forcefully terminates the cue.
  `end` and `kill` can also take a numeric parameter, and will only
  end the cue if it is still running an effect with the specified ID.

  The first outlet sends messages that provide status updates about
  the cue, `started`, `ending`, or `ended`, each followed by the
  numeric ID of the effect associated with that run of the cue. These
  messages will be sent regardless of whether the cue was started by
  this object.

  The remaining inlets and outlets allow the variables bound to the
  cue to be adjusted and monitored. They will be documented once
  implemented."
  {:doc/format :markdown}
  (:gen-class :extends com.cycling74.max.MaxObject
              :constructors {[int int] []}
              :exposes-methods {declareTypedIO parentDeclareTypedIO
                                setInletAssist parentSetInletAssist
                                setOutletAssist parentSetOutletAssist}
              :state state
              :init init
              :post-init post-init
              :main false
              :methods [[start [] void]
                        [end [] void]
                        [end [int] void]
                        [kill [] void]
                        [kill [int] void]])
  (:require [afterglow.max.core :as core]
            [afterglow.controllers :as controllers]
            [afterglow.show :as show]
            [afterglow.show-context :refer [*show*]]
            [taoensso.timbre :as timbre])
  (:import (com.cycling74.max MaxObject Atom)))

(import 'afterglow.max.Cue)

;; TODO: Should we store the constructor value of *show* in our state, in case it changes?
;; TODO: Should we detect changes in the cue stored at our cell, and shut down?

(defn- -init
  [x y]
  (core/init)
  (timbre/info "-init" x y)
  (when (nil? *show*)
    (throw (IllegalStateException. "Cannot create Cue object: No default show has been established.")))
  (let [cue (controllers/cue-at (:cue-grid *show*) x y)]
    (when (nil? cue)
      (throw (IllegalStateException. (str "Cannot create Cue object: No cue at " x ", " y " in *show*"))))
    [[] (atom {:x x :y y :cue cue})]))

(defn- -post-init
  "The post-init phase of the constructors tells Max about the inlets
  and outlets supported by this object, and registers our interest in
  cue state with Afterglow."
  [this x y]
  (timbre/info "-post-init" x y)
  (.parentDeclareTypedIO this "m" "m")
  (.parentSetInletAssist this (into-array String ["start, end, kill: control cue; end and kill can take id"]))
  (.parentSetOutletAssist this (into-array String ["reports changes in cue state and associated id values"]))
  (swap! (.state this) assoc :f (fn [new-state _ id]
                                  (.outlet this 0 (name new-state) (into-array Atom [(Atom/newAtom id)]))))
  (controllers/add-cue-fn! (:cue-grid *show*) x y (:f @(.state this))))

;; TODO: Start it over if it is ending.
(defn- -start
  "Start the cue if it is not already running."
  [this]
  (let [{:keys [x y]} @(.state this)
        [_ active] (show/find-cue-grid-active-effect *show* x y)]
    (when-not active
      (show/add-effect-from-cue-grid! x y))))

(defn- -end
  "Ask the cue to end; if it has already been asked once, kill it
  immediately. If an id parameter is given, the cue will be affected
  only if it is still running the effect with the specified id."
  ([this]
   (-end this nil))
  ([this id]
   (show/end-effect! (get-in @(.state this) [:cue :key]) :when-id id)))

(defn- -kill
  "Terminate the cue immediately. If an id parameter is given, the cue
  will be killed only if it is still running the effect with the
  specified id."
  ([this]
   (-kill this nil))
  ([this id]
  (show/end-effect! (get-in @(.state this) [:cue :key]) :force true :when-id id)))

(defn- -bang
  "Respond to a bang message. If we have a configured expression,
  evaluate it and return the result; otherwise simply pass the bang
  along."
  [this]
  (let [{:keys [x y cue]} @(.state this)
        [_ active] (show/find-cue-grid-active-effect *show* x y)]
    (if active
      (show/end-effect! (:key cue))
      (show/add-effect-from-cue-grid! x y))))

(defn- -notifyDeleted
  "The Max peer object has been deleted, so this instance is no longer
  going to be used. Unregister our cue state notification function,
  and allow this object to be garbage collected."
  [this]
  (let [{:keys [x y f]} @(.state this)]
    (controllers/clear-cue-fn! (:cue-grid *show*) x y f)))
