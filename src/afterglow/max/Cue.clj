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
                                createInfoOutlet parentCreateInfoOutlet
                                getInlet parentGetInlet}
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
;;       For now, no, establish convention that *show* gets set in afterglow-max startup
;;       and is not changed after that point.

;; TODO: Should we detect changes in the cue stored at our cell, and shut down?

(defn cue-variables
  "Return the cue's variables."
  [this]
  (get-in @(.state this) [:cue :variables]))

(defn cue-variable-names
  "Return the user-oriented names of the cue's variables."
  [this]
  (map :name (cue-variables this)))

(defn cue-variable-inlet-assist
  "Return the tooltip text describing the cue variable inlets."
  [this]
  (for [n (cue-variable-names this)]
    (str "float: Set " n "; bang: Report current value")))

(defn cue-variable-outlet-assist
  "Return the tooltip text describing the cue variable outlets."
  [this]
  (for [n (cue-variable-names this)]
    (str "Output changed values of " n)))

(defn cue-variable-type-string
  "Returns the type strings telling Max what kind of outlets each cue
  variable uses."
  [this]
  (clojure.string/join (map #(if (= (:type %) :int) "i" "f") (cue-variables this))))

(defn unwatch-cue-local-variables
  "When a cue has ended, or this object is being deactivated, get rid
  of any callback functions that were watching cue-local variable
  value changes."
  [this]
  (doseq [[k f] (:local-var-fn @(.state this))]
    (show/clear-variable-set-fn! k f))
  (swap! (.state this) dissoc :local-var-fn))

(defn watch-cue-local-variables
  "After we start a cue, this function looks for any variable bindings
  which are cue-local, and registers callback functions to send
  updates if their values change. Also immediately sends out the
  values of the variables that the cue started up with."
  [this]
  (unwatch-cue-local-variables this)  ; Should be unnecessary, but clean up just in case
  (let [{:keys [x y]} @(.state this)
        [_ active] (show/find-cue-grid-active-effect *show* x y)]
    (when active
      (doseq [[i v] (map vector (range) (cue-variables this))]
      (when (string? (:key v))
        (let [outlet (inc i)
              cue-local-key (keyword (:key v))
              cue-local-var (get-in active [:variables cue-local-key])
              f (fn [_ new-value] (when (number? new-value) (.outlet this outlet (float new-value))))]
          (swap! (.state this) assoc-in [:local-var-fn cue-local-var] f)
          (f cue-local-var (show/get-variable cue-local-var))
          (show/add-variable-set-fn! cue-local-var f)))))))

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
  (let [type-string (str "m" (cue-variable-type-string this))
        cue-name (get-in @(.state this) [:cue :name])
        first-inlet-assist (str "start, end, kill, bang: Control cue \"" cue-name "\"")]
    (.parentDeclareTypedIO this type-string type-string)
    (.parentSetInletAssist this (into-array String (concat [first-inlet-assist]
                                                           (cue-variable-inlet-assist this))))
    (.parentSetOutletAssist this (into-array String (concat ["Output changes in Cue state and associated id values"]
                                                            (cue-variable-outlet-assist this))))
    (.parentCreateInfoOutlet this false)

    ;; Set up the callback function for changes to cue state
    (let [f (fn [new-state _ id]
              (.outlet this 0 (name new-state) (into-array Atom [(Atom/newAtom id) (Atom/newAtom cue-name)]))
              (case new-state
                :started (watch-cue-local-variables this)
                :ended (unwatch-cue-local-variables this)
                nil))]
      (swap! (.state this) assoc :cue-fn f)
      (controllers/add-cue-fn! (:cue-grid *show*) x y f))

    ;; Set up the callback functions for changes to values of permanent show variables bound to the cue
    (doseq [[i v] (map vector (range) (cue-variables this))]
      (when (keyword? (:key v))
        (let [outlet (inc i)
              f (fn [_ new-value] (when (number? new-value) (.outlet this outlet (float new-value))))]
          (swap! (.state this) assoc-in [:perm-var-fn (:key v)] f)
          (show/add-variable-set-fn! (:key v) f))))))

(defn apply-cue-local-variables
  "After we start a cue, this function looks for any local values we
  have saved for variables are creted for the duration of the cue,
  and if any are found, copies them into the actual cue variables."
  [this]
  (let [{:keys [x y]} @(.state this)
        [_ active] (show/find-cue-grid-active-effect *show* x y)]
    (when active
      (doseq [v (cue-variables this)]
        (when (string? (:key v))
          (let [cue-local-key (keyword (:key v))
                cue-local-value (get-in @(.state this) [:variables cue-local-key])]
            (when cue-local-value
              (show/set-variable! (get-in active [:variables cue-local-key]) cue-local-value))))))))

(defn- -start
  "Start the cue if it is not already running (if it is in the process
  of ending, start a new instance in its place)."
  [this]
  (let [{:keys [x y]} @(.state this)
        [_ active] (show/find-cue-grid-active-effect *show* x y)]
    (when-not (and active (not (:ending active)))
      (show/add-effect-from-cue-grid! x y)
      (apply-cue-local-variables this))))

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

(defn get-cue-local-variable
  "Look up the current value for a cue variable which gets created for
  the lifespan of a running cue. If the cue is currently active,
  report the value of the corresponding temporary show variable. If
  not, report the local value we are tracking to be used the next time
  we start the cue."
  [this v]
  (let [cue-local-key (keyword (:key v))
        {:keys [x y]} @(.state this)
        [_ active] (show/find-cue-grid-active-effect *show* x y)]
    (if active
      (show/get-variable (get-in active [:variables cue-local-key]))
      (get-in @(.state this) [:variables cue-local-key]))))

(defn- -bang
  "Respond to a bang message. For the cue control inlet, this acts
  like a press on a grid controller pad: If the cue is not active,
  start it. If it is running, ask it to end; if it is still running
  after being asked to end, this will kill it. For variable inlets,
  causes the current value of the variable, if it is defined, to be
  output to the corresponding outlet. If the variable has no value, or
  is not a number, the corresponding outlet is simply banged."
  [this]
  (let [inlet (.parentGetInlet this)
        {:keys [x y cue]} @(.state this)
        [_ active] (show/find-cue-grid-active-effect *show* x y)]
    (if (zero? inlet)  ; Cue control inlet?
      (if active  ; Transition the cue to the next appropriate state.
        (-end this)
        (-start this))
      (let [v (nth (cue-variables this) (dec inlet)) ; One of the variable inlets; output its current value.
            current-val (if (keyword? (:key v))
                          (show/get-variable (:key v))
                          (get-cue-local-variable this v))]
        (if (number? current-val)
          (.outlet this inlet (float current-val))
          (.outletBang this inlet))))))

(defn update-cue-local-variable
  "Max wants to set a value for a cue variable which gets created for
  the lifespan of a running cue. Save a local copy to use when
  starting up future cues, and if there is currently one active, also
  update the corresponding temporary show variable."
  [this new-value v]
  (let [cue-local-key (keyword (:key v))
        {:keys [x y]} @(.state this)
        [_ active] (show/find-cue-grid-active-effect *show* x y)]
    (swap! (.state this) assoc-in [:variables cue-local-key] new-value)
    (when active (show/set-variable! (get-in active [:variables cue-local-key]) new-value))))

(defn- -inlet-float
  "Respond to a float message, meaning the value of one of the
  variable inlets has been set."
  [this new-value]
  (let [inlet (.parentGetInlet this)]
    (if (zero? inlet)  ; Only variable inlets accept floats, ignore attempts to send to 0, but complain.
      (timbre/error "Cue control inlet does not respond to numbers.")
      (let [v (nth (cue-variables this) (dec inlet))
            new-value (max new-value (:min v))  ; Restrict range of variable values as per cue specs.
            new-value (min new-value (:max v))]
        (if (keyword? (:key v))
          (show/set-variable! (:key v) new-value)
          (update-cue-local-variable this new-value v))
        (.outlet this inlet new-value)))))

(defn- -notifyDeleted
  "The Max peer object has been deleted, so this instance is no longer
  going to be used. Unregister our cue state notification function,
  and any variable change notification functions, and allow this
  object to be garbage collected."
  [this]
  (let [{:keys [x y cue-fn perm-var-fn]} @(.state this)]
    (controllers/clear-cue-fn! (:cue-grid *show*) x y cue-fn)
    (doseq [[k f] perm-var-fn]
      (show/clear-variable-set-fn! k f))
    (unwatch-cue-local-variables this)))
