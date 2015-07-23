(ns afterglow.max.Cue
  "This implements a class that allows https://cycling74.com[Max] from
  Cycling '74 to interact with Afterglow cue grid entries.

  It can only be instantiated when Afterglow is hosted as a Max
  package, because it relies on classes provided by Max which are not
  distributed as part of Afterglow." {:doc/format :markdown}
  (:gen-class :extends com.cycling74.max.MaxObject
              :constructors {[] []}
              :init init
              :main false)
  (:import (com.cycling74.max MaxObject)))

(import 'afterglow.max.Cue)

(defn- -init
  []
  (MaxObject/post (str "afterglow-max says hello world! java version:" (System/getProperty "java.version"))))

