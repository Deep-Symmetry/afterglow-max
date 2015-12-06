(ns afterglow.max.Eval
  "This implements a class that allows https://cycling74.com[Max] from
  Cycling '74 to evaluate http://clojure.org[Clojure] expressions
  within a hostend instance of
  https://github.com/brunchboy/afterglow#afterglow[Afterglow].

  Allows the evaluation of a preconfigured Clojure expression when a
  `bang` is received, or an arbitrary expression sent as a string
  argument to an `eval` message. Each instance maintains its own
  evaluation context and thread-local bindings (for things like
  `*ns*`, the current namespace, etc.) with the help of the Afterglow
  web REPL implementation, but unlike an actual web repl, there is no
  session to expire. The bindings are kept around as long as the
  object exists in an active Max patcher."
  (:gen-class :extends com.cycling74.max.MaxObject
              :constructors {[] []
                             [String] []}
              :exposes-methods {declareTypedIO parentDeclareTypedIO
                                setInletAssist parentSetInletAssist
                                setOutletAssist parentSetOutletAssist}
              :state state
              :init init
              :post-init post-init
              :main false
              :methods [[eval [String] void]])
  (:require [afterglow.max.core :as core]
            [afterglow.web.routes.web-repl :as web-repl]
            [taoensso.timbre :as timbre])
  (:import (com.cycling74.max MaxObject)))

(import 'afterglow.max.Eval)

(defn- -init
  "The init phase of the constructors sets the default expression to
  be evaluated when a bang message is received."
  ([]
   (-init nil))
  ([expr]
   (core/init)
   [[] expr]))

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
  "Evaluate an expression and send the result to the outlet.
  We leverage the infrastructure built into the web console for
  maintaining an evaluation context."
  [this expr]
  (let [result (web-repl/do-eval expr this)]
    (if (:result result)
      (.outlet this 0 "result" (:result result))
      (timbre/error (str "Problem evaluating " expr ": " (:error result))))))

(defn- -bang
  "Respond to a bang message. If we have a configured expression,
  evaluate it and return the result; otherwise simply pass the bang
  along."
  [this]
  (if-let [expr (.state this)]
    (-eval this expr)
    (.outletBang this 0)))

(defn- -notifyDeleted
  "The Max peer object has been deleted, so this instance is no longer
  going to be used. Tell the Afterglow web REPL it can clean up our
  thread-local bindings."
  [this]
  (web-repl/discard-bindings this))
