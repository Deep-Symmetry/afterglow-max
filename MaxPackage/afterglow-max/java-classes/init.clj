(ns afterglow.max.init
  "The sample contents of this file creates the a demo show and set up
  the standard Afterglow sample cues. You should edit it to create
  your own actual show and cues.

  It also loads your local fixture definitions from the fixtures.clj
  file in the same directory."
  (:require [afterglow.examples :as examples]
            [afterglow.effects.params :as params]
            [afterglow.fixtures.american-dj :as adj]
            [afterglow.fixtures.blizzard :as blizzard]
            [afterglow.fixtures.chauvet :as chauvet]
            [afterglow.show :as show]
            [afterglow.show-context :refer :all]
            [afterglow.transform :as tf]))

;; Load any local fixture definitions
(load-init-file "fixtures.clj")
(alias 'my-fixtures 'afterglow.max.fixtures)

;; Thanks to the above two lines, you can now access any
;; fixtures defined in the local fixtures.clj file like
;; so:
;;  (my-fixtures/awesome-light)
;; assuming you had defined a fixture named awesome-light.

;; Set up an atom to hold the demo show once it is created:
(defonce ^{:doc "Holds the sample show if it has been created,
  so it can be unregistered if it is beign re-created."}
  demo-show
  (atom nil))

;; This function sets up a demo show. When you are ready, edit it to
;; contain the actual lights you have, using your new fixture definitions
;; in fixtures.clj, patched on the actual DMX addresses they use, and at
;; the actual physical locations and orientations that you have them set
;; up.
(defn use-demo-show
  "Set up a demo show for experimenting with afterglow-max. By default
  it will create the show to use universe 1, but if you want to use a
  different universe (for example, a dummy universe on ID 0, because
  your DMX interface isn't handy right now), you can override that by
  supplying a different ID after :universe."
  [& {:keys [universe] :or {universe 1}}]
  ;; Create, or re-create the show, on the choen OLA universe, for demonstration
  ;; purposes. Make it the default show so we don't need to wrap everything below
  ;; in a (with-show sample-show ...) binding
  (set-default-show! (swap! demo-show (fn [s]
                                        (when s
                                          (show/unregister-show s)
                                          (with-show s (show/stop!)))
                                        (show/show :universes [universe] :description "Max Demo Show"))))

  ;; Throw a couple of fixtures in there to play with. For better fun, use
  ;; fixtures and addresses that correspond to your actual hardware.
  (show/patch-fixture! :torrent-1 (blizzard/torrent-f3) universe 1
                       :x (tf/inches 50) :y (tf/inches 43) :z (tf/inches 0)
                       :y-rotation (tf/degrees 0))
  (show/patch-fixture! :torrent-2 (blizzard/torrent-f3) universe 17
                       :x (tf/inches 50) :y (tf/inches 43) :z (tf/inches 239)
                       :y-rotation (tf/degrees -90))
  (show/patch-fixture! :hex-1 (chauvet/slimpar-hex3-irc) universe 129 :x (tf/inches 14) :y (tf/inches 44)
                       :z (tf/inches -4.75)
                       :x-rotation (tf/degrees 90))
  (show/patch-fixture! :hex-2 (chauvet/slimpar-hex3-irc) universe 145 :x (tf/inches -14) :y (tf/inches 44)
                       :z (tf/inches 124)
                       :x-rotation (tf/degrees 90))
  (show/patch-fixture! :blade-1 (blizzard/blade-rgbw) universe 225
                       :x (tf/inches -27) :y (tf/inches 41) :z (tf/inches 4)
                       :y-rotation (tf/degrees -45))
  (show/patch-fixture! :blade-2 (blizzard/blade-rgbw) universe 240
                       :x (tf/inches 26.5) :y (tf/inches 48.5) :z (tf/inches -4.75)
                       :y-rotation (tf/degrees 45))
  (show/patch-fixture! :blade-3 (blizzard/blade-rgbw) universe 255
                       :x (tf/inches 13) :y (tf/inches 34) :z (tf/inches 239)
                       :y-rotation (tf/degrees 47))
  (show/patch-fixture! :blade-4 (blizzard/blade-rgbw) universe 270 :y (tf/inches 48.5) :z (tf/inches -4.75))
  (show/patch-fixture! :ws-1 (blizzard/weather-system) universe 161
                       :x (tf/inches 0) :y (tf/inches 71) :z (tf/inches 0) :y-rotation (tf/degrees 0))
  (show/patch-fixture! :ws-2 (blizzard/weather-system) universe 187
                       :x (tf/inches 0) :y (tf/inches 71) :z (tf/inches 220) :y-rotation (tf/degrees 180))
  (show/patch-fixture! :puck-1 (blizzard/puck-fab5) universe 97 :x (tf/inches -76) :y (tf/inches 8) :z (tf/inches 52))
  (show/patch-fixture! :puck-2 (blizzard/puck-fab5) universe 113 :x (tf/inches 76) :y (tf/inches 8) :z (tf/inches 40))
  (show/patch-fixture! :snowball (blizzard/snowball) universe 33 :x (tf/inches -76) :y (tf/inches 32)
                       :z (tf/inches 164.5))
  (show/patch-fixture! :hyp-rgb (adj/hypnotic-rgb) universe 45)

  ;; Turn on the OSC server.
  (when (nil? @afterglow.core/osc-server)
    (afterglow.core/start-osc-server 16010))

  '*show*)

;; Create the sample show that ships with Afterglow, because the cues depend on it.
;; Replace this with a call to create your own show when you are ready.
(use-demo-show)

;; Create the standard Afterglow sample cues within the demo show cue grid.
(examples/make-cues false)

;; Add your own cues and effects here, using the source for examples/make-cues as a guide...
;; Once you have enough, you may no longer need the example ones.
