(ns afterglow.max.core
  (:require [afterglow.core]
            [afterglow.max.init :refer [init-dir load-init-file]]
            [taoensso.timbre :as timbre])
  (:import (com.cycling74.max MaxObject DataTypes)
           (afterglow.max init__init)))

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
         (if (:error-level? data)
           (MaxObject/error output-str)
           (MaxObject/post output-str))
         (catch Throwable t
           (try
             (.println System/err t)
             (catch Throwable _))))))})

(defn- init-internal
  "Performs the actual initialization, protected by the delay below
  to insure it happens only once."
  []
  (afterglow.core/init-logging {:max (max-console-appender)})
  (try
    ;; Unfortunately,  Max 7 on the Mac (and maybe other versions?) does not actually put anything but
    ;; the JAR files on the class path, even though it claims to. So the following line, which should
    ;; simply work, does not, and we need to do all the mumbo jumbo you see after it instead.
    ;;(require 'afterglow-max-init)
    (let [jar (clojure.java.io/file (.getLocation (.getCodeSource (.getProtectionDomain afterglow.max.init__init))))
          dir (.getParentFile (.getParentFile jar))]
      (reset! init-dir dir)
      (binding [*ns* (the-ns 'afterglow.max.init)]
        (load-init-file "init.clj")))
    (catch Throwable t
      (timbre/error "Problem loading Afterglow configuration file init.clj:" t))))

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
