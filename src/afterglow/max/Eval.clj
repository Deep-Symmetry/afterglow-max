(ns afterglow.max.Eval
  "This implements a class that allows https://cycling74.com[Max] from
  Cycling '74 to evaluate http://clojure.org[Clojure] expressions
  within a hostend instance of
  https://github.com/brunchboy/afterglow#afterglow[Afterglow].

  It can only be instantiated when Afterglow is hosted as a Max
  package, because it relies on classes provided by Max which are not
  distributed as part of Afterglow." {:doc/format :markdown}
  (:gen-class :extends com.cycling74.max.MaxObject
              :constructors {[] []
                             [String] []}
              :exposes-methods {declareTypedIO parentDeclareTypedIO
                                setInletAssist parentSetInletAssist
                                setOutletAssist parentSetOutletAssist
                                bang parentBang}
              :state default-expr
              :init init
              :post-init post-init
              :main false
              :methods [[eval [String] void]])
  (:require [afterglow.max.core :as core]
            [taoensso.timbre :as timbre])
  (:import (com.cycling74.max MaxObject DataTypes)))

(import 'afterglow.max.Eval)

(defn- -init
  "The init phase of the constructors sets the default expression to
  be evaluated when a bang message is received."
  ([]
   (-init nil))
  ([expr]
   ()
   expr))

(defn- -post-init
  "The post-init phase of the constructors tells Max
  about the inlet and outlet supported by this object."
  ([this]
   (-post-init this nil))
  ([this expr]
   (.parentDeclareTypedIO this "m" "m")
   (.parentSetInletAssist this (into-array String ["eval: evaluate Clojure expression within Afterglow"]))
   (.parentSetOutletAssist this (into-array String ["result: evaluated expression"]))))

(defn- -eval
  "Evaluate an expression and send the result to the outlet"
  [this expr]
  (MaxObject/post (str "eval: " expr)))
