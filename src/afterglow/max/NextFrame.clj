(ns afterglow.max.NextFrame
  "This implements a class that allows https://cycling74.com[Max] from
  Cycling '74 to prepare for the next frame of a light show that is
  about to be rendered by
  https://github.com/brunchboy/afterglow#afterglow[Afterglow]. Shortly
  before that frame is rendered, the right outlet sends a list
  containing information about the show metronome: its current beats
  per minute, beats per bar, and beats per phrase, the instant when
  the first beat started, and the instant at which the frame is going
  to be rendered. Then the left outlet sends out a list of values
  describing the beat, bar, and phrase that will be in effect when
  that frame is rendered, followed by the beat phase, bar phase, and
  phrase phase.

  The inlet can be used to start or stop the
  https://github.com/brunchboy/afterglow/blob/master/doc/rendering_loop.adoc#the-rendering-loop[Rendering
  Loop] of the show. Sending a `start` message makes sure it is
  running; sending a `stop` message shuts it down and blacks out the
  associated lighting universes. Sending a `bang` toggles between
  those states."
  (:gen-class :extends com.cycling74.max.MaxObject
              :constructors {[] []}
              :exposes-methods {declareTypedIO parentDeclareTypedIO
                                setInletAssist parentSetInletAssist
                                setOutletAssist parentSetOutletAssist
                                createInfoOutlet parentCreateInfoOutlet}
              :state state
              :init init
              :post-init post-init
              :main false
              :methods [[start [] void]
                        [stop [] void]])
  (:require [afterglow.max.core :as core]
            [afterglow.rhythm :as rhythm]
            [afterglow.show :as show]
            [afterglow.show-context :refer [*show*]]
            [taoensso.timbre :as timbre])
  (:import (com.cycling74.max MaxObject DataTypes Atom)))

(defn -init
  "Called during the first phase of object construction; allocates
  object state."
  []
  (core/init)
  (when (nil? *show*)
    (throw (IllegalStateException. "Cannot create NextFrame object: No default show has been established.")))
  [[] (atom nil)])

(defn send-frame-update
  "Given a snapshot for an upcoming frame, send the details out the
  outlets."
  [this snapshot]
  (.outletHigh this 2 (into-array Atom [(Atom/newAtom (float (:bpm snapshot)))
                                    (Atom/newAtom (:bpb snapshot))
                                    (Atom/newAtom (:bpp snapshot))]))
  (.outletHigh this 1 (into-array Atom [(Atom/newAtom (float (:phrase-phase snapshot)))
                                    (Atom/newAtom (float (:bar-phase snapshot)))
                                    (Atom/newAtom (float (:beat-phase snapshot)))]))
  (.outletHigh this 0 (into-array Atom [(Atom/newAtom (:phrase snapshot))
                                    (Atom/newAtom (rhythm/snapshot-bar-within-phrase snapshot))
                                    (Atom/newAtom (rhythm/snapshot-beat-within-bar snapshot))]))  )

(defn -post-init
  "The post-init phase of the constructors tells Max about the inlets
  and outlets supported by this object, and registers our function to
  get called with information about upcoming frames to be rendered."
  [this]
  (.parentDeclareTypedIO this "m" "lll")
  (.parentSetInletAssist this (into-array String ["start, stop: Control the Show"]))
  (.parentSetOutletAssist this (into-array String ["phrase, bar, beat"
                                                   "phrase phase, bar phase, beat phase"
                                                   "bpm, bpb, bpp"]))
  (.parentCreateInfoOutlet this false)

  ;; Set up the callback function for upcoming frames
  (let [f (fn [snapshot] (send-frame-update this snapshot))]
    (reset! (.state this) f)
    (show/add-frame-fn! f)))

(defn -start
  "Start the show's
  https://github.com/brunchboy/afterglow/blob/master/doc/rendering_loop.adoc#the-rendering-loop[Rendering
  Loop] if it is not already running."
  [this]
  (show/start!))

(defn -stop
  "Stop the show's
  https://github.com/brunchboy/afterglow/blob/master/doc/rendering_loop.adoc#the-rendering-loop[Rendering
  Loop] and black out all of its lighting universes."
  [this]
  (show/stop!)
    (Thread/sleep (:refresh-interval *show*))
    (show/blackout-show))

(defn -bang
  "Toggles the show between started and stopped."
  [this]
  (if (show/running?)
    (-stop this)
    (-start this)))

(defn -notifyDeleted
  "The Max peer object has been deleted, so this instance is no longer
  going to be used. Unregister our frame notification function and
  allow this object to be garbage collected."
  [this]
  (show/clear-frame-fn! @(.state this)))
