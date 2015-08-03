(ns afterglow.max.fixtures
  "This file is for you to add any fixture definitions you need to
  control the lights that make up your show. See the Getting Started
  discussion linked from any of the afterglow-max object Reference
  pages in Max for details and suggestions on how to get help if you
  need it.

  If you do create your own fixture definitions, please consider
  submitting them for inclusion with Afterglow, so that others can
  benefit from your work. You can either submit a [Pull
  Request](https://github.com/brunchboy/afterglow/pulls) if you think
  your definition is correct and complete, or just post your work in
  progress to
  the [wiki](https://github.com/brunchboy/afterglow/wiki/Questions)
  and perhaps someone can help you finish it off."
  {:doc/format :markdown}
  (:require [afterglow.channels :as chan]
            [afterglow.effects.channel :refer [function-value-scaler]]
            [taoensso.timbre :as timbre]))


