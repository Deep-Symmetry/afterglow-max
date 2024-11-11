(ns afterglow.max.Metro
  "This implements a class that allows https://cycling74.com[Max] to
  adjust the state of, and obtain giming information from, an
  Afterglow
  https://github.com/brunchboy/afterglow/blob/master/doc/metronomes.adoc#metronomes[Metronome].
  The associated metronome may be the default metronome of a show, or
  stored in a named show variable.

  Configured with the keyword that identifies the variable within the
  show, as you would use with <<show/set-variable!>>, when you want to
  use a metronome other than the default one.

  The inlets and outlet allow the chosen metronome to be adjusted and
  monitored. The inlets responds to `int` (or `float` for the first
  inlet only) messages by setting the corresponding metronome state
  directly, and respond to `tap` messages by adjusting it in a
  relative way. In order from left to right, the inlets control BPM,
  the current phrase number, the bar within the phrase, and the beat
  within the bar. Seind a `bang` message to any inlet causes the
  current metronome timing information to be sent out all outlets. The
  outlets underneath inlets report on the same value controlled by
  that inlet, while the rightmost outlet reports a list containing the
  current phases of the phrase, bar, and beat."
  (:gen-class :extends com.cycling74.max.MaxObject
              :constructors {[String] []}
              :exposes-methods {declareTypedIO parentDeclareTypedIO
                                setInletAssist parentSetInletAssist
                                setOutletAssist parentSetOutletAssist
                                createInfoOutlet parentCreateInfoOutlet
                                getInlet parentGetInlet}
              :state state
              :init init
              :post-init post-init
              :main false
              :methods [[tap [] void]])
  (:require [afterglow.max.core :as core]
            [afterglow.midi :as amidi]
            [afterglow.rhythm :as rhythm]
            [afterglow.show :as show]
            [afterglow.show-context :refer [*show* with-show]]
            [clojure.string :as string]
            [taoensso.timbre :as timbre])
  (:import (com.cycling74.max MaxObject Atom)))

(defn -init
  "Called during the first phase of object construction with the
  keyword of the variable to bind to, if any. Allocates object state."
  [k]
  (core/init)
  (when (nil? *show*)
    (throw (IllegalStateException. "Cannot create Metro object: No default show has been established.")))
  (when (and (not (string/blank? k))
             (not (satisfies? rhythm/IMetronome (show/get-variable k))))
    (throw (IllegalStateException. (str "Cannot create Metro object: Variable " (keyword k)
                                        " does not contain a Metronome."))))
  (let [metro (if (clojure.string/blank? k) (:metronome *show*) (show/get-variable k))]
    [[] {:metronome metro
         :show *show*
         :tap-tempo-handler (amidi/create-tempo-tap-handler metro)}]))

(defn -post-init
  "The post-init phase of the constructors tells Max about the inlets
  and outlets supported by this object."
  [this k]
  (.parentDeclareTypedIO this "mmmm" "fiiil")
  (.parentSetInletAssist this (into-array String ["float: Set BPM; tap: Tap tempo; bang: Report all"
                                                  "int: Set phrase #; tap: Start phrase; bang: report all"
                                                  "int: Set bar #; tap: Start bar; bang: report all"
                                                  "int: Set beat #; tap: Start beat; bang: report all"]))
  (.parentSetOutletAssist this (into-array String ["current BPM" "current phrase #" "current bar within phrase"
                                                   "current beat within bar", "phrase phase, bar phase, beat phase"]))
  (.parentCreateInfoOutlet this false))

(defn -bang
  "Respond to a bang message. Sends the current metronome state to
  all outlets."
  [this]
  (let [snap (rhythm/metro-snapshot (:metronome (.state this)))]
    (.outlet this 0 (float (:bpm snap)))
    (.outlet this 1 (int (:phrase snap)))
    (.outlet this 2 (int (rhythm/snapshot-bar-within-phrase snap)))
    (.outlet this 3 (int (rhythm/snapshot-beat-within-bar snap)))
    (.outlet this 4 (into-array Atom [(Atom/newAtom (float (:phrase-phase snap)))
                                      (Atom/newAtom (float (:bar-phase snap)))
                                      (Atom/newAtom (float (:beat-phase snap)))]))))

(defn set-bpm
  "Set the BPM of the metronome to a specific number, unless it is
  currently synced to a tempo source."
  [this bpm]
  (when (= (:type (show/sync-status)) :manual)
    (rhythm/metro-bpm (:metronome (.state this)) bpm)))

(defn set-bar
  "Set the bar within the phrase."
  [this bar]
  (let [metro (:metronome (.state this))
        snap (rhythm/metro-snapshot metro)
        bar (mod bar (:bpp snap))
        delta (- (rhythm/snapshot-bar-within-phrase snap) bar)]
    (when-not (zero? delta)
      (rhythm/metro-adjust metro (* delta (rhythm/metro-tock metro))))))

(defn set-beat
  "Set the beat within the bar."
  [this beat]
  (let [metro (:metronome (.state this))
        snap (rhythm/metro-snapshot metro)
        beat (mod beat (:bpb snap))
        delta (- (rhythm/snapshot-beat-within-bar snap) beat)]
    (when-not (zero? delta)
      (rhythm/metro-adjust metro (* delta (rhythm/metro-tick metro))))))

(defn -inlet-int
  "Respond to an int message, setting the corresponding metronome
  parameter."
  [this new-value]
  (case (.parentGetInlet this)
    0 (set-bpm this new-value)
    1 (rhythm/metro-phrase-start (:metronome (.state this)) new-value)
    2 (set-bar this new-value)
    3 (set-beat this new-value))
  (-bang this))

;; TODO: accept a list in the first inlet, to set the bars per phrase and beats per bar as well as BPM.

(defn -inlet-float
  "Respond to a float message, setting the corresponding metronome
  parameter."
  [this new-value]
  (if (zero? (.parentGetInlet this))
    (do (set-bpm this new-value)
        (-bang this))
    (-inlet-int this (int new-value))))

(defn interpret-tempo-tap
  "React appropriately to a tempo tap, based on the sync mode of the
  metronome. If it is manual, invoke the metronome tap-tempo handler.
  Otherwise, ignore."
  [this]
  (with-show (:show (.state this))
    (when (=  (:type (show/sync-status)) :manual)
      ((:tap-tempo-handler (.state this))))))

(defn interpret-beat-tap
  "React appropriately to a tap on the beat inlet, based on the sync
  mode of the metronome. If it is manual or MIDI, realign the beat to
  start now. Otherwise, ignore."
  [this]
  (with-show (:show (.state this))
    (when (#{:manual :midi} (:type (show/sync-status)))
      (rhythm/metro-beat-phase (:metronome (.state this)) 0))))

(defn -tap
  "Respond to a tap message, adjusting the corresponding metronome
  parameter."
  [this]
  (let [metro (:metronome (.state this))]
    (case (.parentGetInlet this)
      0 (interpret-tempo-tap this)
      1 (rhythm/metro-phrase-start metro (rhythm/metro-phrase metro))
      2 (rhythm/metro-bar-start metro (rhythm/metro-bar metro))
      3 (interpret-beat-tap this)))
  (-bang this))
