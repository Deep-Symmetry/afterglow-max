(ns afterglow.max.init
  "This namespace is the context in which the init.clj file will be
  loaded when the first afterglow-max object is created by mxj, to
  initialize the Afterglow environment."
  (:require [afterglow.show-context :refer [*show* set-default-show!]]))

(defonce ^{:doc "Will be set to contain a java.io.File object
  identifying the directory from which the init.clj file is being
  loaded, to help it load other files using [[load-init-file]]."
           :doc/format :markdown}
  init-dir (atom nil))

(defn load-init-file
  "Load the specified file from the java-classes directory which
  contains the init.clj processed during afterglow-max startup.
  This function relies on [[init-dir]] being set by the startup
  process."
  [filename]
  (load-file (.getPath (clojure.java.io/file @init-dir filename))))

