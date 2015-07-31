(ns afterglow.max.init
  "This sample file just creates the standard Afterglow sample show and cues.
  You should change it to create your own actual show and cues."
  (:require [afterglow.examples :as examples]
            [afterglow.effects.params :as params]
            [afterglow.fixtures.american-dj :as adj]
            [afterglow.fixtures.blizzard :as blizzard]
            [afterglow.fixtures.chauvet :as chauvet]
            [afterglow.show :as show]
            [afterglow.show-context :refer :all]
            [afterglow.transform :as tf]))

(defonce ^{:doc "Holds the sample show if it has been created,
  so it can be unregistered if it is beign re-created."}
  demo-show
  (atom nil))

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
                                        (show/show :universes [universe]))))

  ;; TODO: Should this be automatic? If so, creating the show should assign the name too.
  ;; Register it with the web interface.
  (show/register-show *show* "Demo Show")

  ;; Throw a couple of fixtures in there to play with. For better fun, use
  ;; fixtures and addresses that correspond to your actual hardware.
  (show/patch-fixture! :torrent-1 (blizzard/torrent-f3) universe 1
                       :x (tf/inches 25) :y (tf/inches 12) :z 0
                       :y-rotation (tf/degrees 0))
  (show/patch-fixture! :torrent-2 (blizzard/torrent-f3) universe 17
                       :x (tf/inches -44) :y (tf/inches 51.75) :z (tf/inches -4.75)
                       :y-rotation (tf/degrees 0))
  (show/patch-fixture! :hex-1 (chauvet/slimpar-hex3-irc) universe 129 :x (tf/inches 14) :y (tf/inches 44)
                       :z (tf/inches -4.75)
                       :x-rotation (tf/degrees 90))
  (show/patch-fixture! :hex-2 (chauvet/slimpar-hex3-irc) universe 145 :x (tf/inches -14) :y (tf/inches 44)
                       :z (tf/inches -4.75)
                       :x-rotation (tf/degrees 90))
  (show/patch-fixture! :blade-1 (blizzard/blade-rgbw) universe 225
                       :x (tf/inches 26.5) :y (tf/inches 48.5) :z (tf/inches -4.75)
                       :y-rotation (tf/degrees -45))
  (show/patch-fixture! :blade-2 (blizzard/blade-rgbw) universe 240
                       :x (tf/inches -26.5) :y (tf/inches 48.5) :z (tf/inches -4.75)
                       :y-rotation (tf/degrees 45))
  (show/patch-fixture! :blade-3 (blizzard/blade-rgbw) universe 255
                       :x (tf/inches 0) :y (tf/inches 38.75) :z (tf/inches 207.5)
                       :y-rotation (tf/degrees 180))
  (show/patch-fixture! :blade-4 (blizzard/blade-rgbw) universe 270 :y (tf/inches 48.5) :z (tf/inches -4.75))
  (show/patch-fixture! :ws-1 (blizzard/weather-system) universe 161
                       :x (tf/inches 55) :y (tf/inches 71) :z (tf/inches 261) :y-rotation (tf/degrees 225))
  (show/patch-fixture! :ws-2 (blizzard/weather-system) universe 187
                       :x (tf/inches -55) :y (tf/inches 71) :z (tf/inches 261) :y-rotation (tf/degrees 135))
  (show/patch-fixture! :puck-1 (blizzard/puck-fab5) universe 97 :x (tf/inches -76) :y (tf/inches 8) :z (tf/inches 52))
  (show/patch-fixture! :puck-2 (blizzard/puck-fab5) universe 113 :x (tf/inches -76) :y (tf/inches 8) :z (tf/inches 40))
  (show/patch-fixture! :snowball (blizzard/snowball) universe 33 :x (tf/inches -76) :y (tf/inches 32)
                       :z (tf/inches 164.5))
  (show/patch-fixture! :hyp-rgb (adj/hypnotic-rgb) universe 45)
  '*show*)

(use-demo-show)
(examples/make-cues)
