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
                                setOutletAssist parentSetOutletAssist
                                createInfoOutlet parentCreateInfoOutlet}
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

(defn cue-variables
  "Return the cue's variables, in sorted order by key."
  [this]
  (sort-by :key (get-in @(.state this) [:cue :variables])))

(defn cue-variable-names
  "Return the user-oriented names of the cue's variable, in key
  order."
  [this]
  (map :name (cue-variables this)))

(defn cue-variable-type-string
  "Returns the type strings telling Max what kind of outlets each cue
  variable uses."
  [this]
  (clojure.string/join (map #(if (= (:type %) :int) "i" "f") (cue-variables this))))

(defn- -init
  [x y]
  (core/init)
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
  (let [type-string (str "m" (cue-variable-type-string this))]
    (.parentDeclareTypedIO this type-string type-string))
  (.parentSetInletAssist this (into-array String (concat ["start, end, kill: control cue; end and kill can take id"]
                                                         (cue-variable-names this))))
  (.parentSetOutletAssist this (into-array String (concat ["reports changes in cue state and associated id values"]
                                                          (cue-variable-names this))))
  (.parentCreateInfoOutlet this false)
  (swap! (.state this) assoc :f (fn [new-state _ id] ; TODO: Include readable effect name as another atom
                                  (let [cue-name (get-in @(.state this) [:cue :name])]
                                    (.outlet this 0 (name new-state)
                                             (into-array Atom [(Atom/newAtom id) (Atom/newAtom cue-name)])))))
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
  "Respond to a bang message. Act like a press on a grid controller
  pad: If the cue is not active, start it. If it is running, ask it to
  end; if it is still running after being asked to end, this will kill
  it."
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
