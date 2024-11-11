(ns afterglow.max.core
  (:require [afterglow.core]
            [afterglow.midi :as amidi]
            [afterglow.version :as version]
            [afterglow.max.init :refer [init-dir load-init-file get-log-path]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.3rd-party.rotor :as rotor])
  (:import (afterglow.max init__init)
           (com.cycling74.max MaxObject DataTypes)))

(defn max-output-fn
  "Format log lines appropriately for the Max console"
  [data]
  (let [{:keys [level ?err_ vargs_ msg_ ?ns-str hostname_ timestamp_]} data]
    (str
     (force timestamp_) " "
     (string/upper-case (name level))  " "
     "[" (or ?ns-str "?ns") "] - "
     (force msg_)
     (when-let [err (force ?err_)]
       (str "\n" (timbre/stacktrace err))))))

(defn max-console-appender
  "Returns an appender which writes to the Max console."
  []
  {:enabled?   true
   :async?     false
   :min-level  nil
   ;; Limit output to three every 30ms, and no more than ten every 30 seconds.
   :rate-limit [[3 30] [10 30000]]
   :output-fn  max-output-fn
   :fn
   (fn [data]
     (let [{:keys [output-fn]} data
           output-str (output-fn data)]
       (try
         (if (:error-level? data)
           (MaxObject/error output-str)
           (MaxObject/post output-str))
         (catch Throwable t
           (try
             (.println System/err t)
             (catch Throwable _))))))})

(declare reload-init)

(defn- init-internal
  "Performs the actual initialization, protected by the delay below
  to insure it happens only once."
  []
  ;; Locate the jar we are running from, so from there we can find our init files and
  ;; log directory.
  (try
    (let [jar (io/file (.getLocation (.getCodeSource (.getProtectionDomain afterglow.max.init__init))))
          dir (.getParentFile (.getParentFile jar))]
      (reset! init-dir dir))
    (catch Throwable t
      (timbre/error "Problem loacating afterglow-max.jar, won't be able to find init files or log directory:" t)))

  (afterglow.core/init-logging (merge {:max (max-console-appender)}
                                      (when @init-dir {:rotor (rotor/rotor-appender {:path (get-log-path)
                                                                                     :max-size 100000
                                                                                     :backlog 5})})))

  ;; Load the MIDI system in a background thread, since it takes a while. This will speed up later
  ;; operations like opening the web show control interface, or activating an Ableton Push controller.
  (future
    (amidi/open-inputs-if-needed!)
    (amidi/open-outputs-if-needed!))

  (try
    ;; Unfortunately,  Max 7 on the Mac (and maybe other versions?) does not actually put anything but
    ;; the JAR files on the class path, even though it claims to. So the following line, which should
    ;; simply work, does not, and we need to do all the mumbo jumbo you see around it instead.
    ;;(require 'afterglow-max-init)
    (reload-init)
    (timbre/info (version/title) (version/tag) "loaded.")
    (catch Throwable t
      (timbre/error "Problem loading afterglow-max configuration file init.clj:" t))))

(defonce ^{:private true
           :doc "Used to ensure initialization takes place exactly once."}
  initialized (delay (init-internal)))

(defn init
  "Makes sure the Afterglow environment has been set up for use with
  Max, including evaluating any initialization forms in init.clj, and
  directing log output to the Max console."
  []
  @initialized)

(defn reload-init
  "Loads the afterglow-max initializaton file init.clj with the
  current namespace set to afterglow.max.init. This function only
  works once [[init]] has prepared the necessary context, but it
  can be used to reload the initialization file without quitting and
  restarting Max."
  []
  (binding [*ns* (the-ns 'afterglow.max.init)]
    (load-init-file "init.clj")))
