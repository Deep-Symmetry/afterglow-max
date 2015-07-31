# afterglow-max

A package for hosting
[Afterglow](https://github.com/brunchboy/afterglow#afterglow) inside
[Cycling ‘74’s Max](https://cycling74.com).

## Installation

1. [Install OLA](https://www.openlighting.org/ola/getting-started/downloads/).
   Since afterglow-max embeds
   [Afterglow](https://github.com/brunchboy/afterglow#afterglow), it
   has the same dependency on the Open Lighting Architecture, so you
   will need that installed before you can use it. (On the Mac I
   recommend using [Homebrew](http://brew.sh) which lets you simply
   `brew install ola`). Once you launch the `olad` server you can
   interact with its embedded
   [web server](http://localhost:9090/ola.html), which is very helpful
   in seeing whether anything is working; you can even watch live DMX
   values changing.

2. Set up an OLA universe for afterglow-max to use. The demonstration
   patchers and help files use universe `1` by default, so the easiest
   thing to do is set that up as a dummy universe. Of course, if you
   have an actual lighting interface and fixtures you want to play
   with, configure the universe to talk to them! The
   [Using OLA](https://www.openlighting.org/ola/getting-started/using-ola/)
   page walks through doing this using the command-line tools; you may
   find it easier to use the web interface, especially the
   [New UI](http://localhost:9090/new/) (the link will work only once
   you have `olad` running). Even though the new UI is technically in
   beta, I have found it completely stable and more friendly and easy
   to work with than the older one.

3. Download a release (this will be a link as soon as the first
   release happens), unzip it, and install the `afterglow-max` folder
   into Max's `Max 7/Packages` folder (in your `Documents` folder).
   Afterglow may also work with Max 6, or even Pure Data, but it
   hasn't been tested with them. If you try, please let us know how it
   goes on the [Wiki](https://github.com/brunchboy/afterglow-max/wiki)!

You should then be able to launch Max and experiment with the
afterglow-max objects! Once you want to start patching your own
fixtures and creating your own custom cues and effects within the
Clojure side of afterglow-max, you will want to edit the file
`afterglow-max/java-classes/init.clj`.

If you want to build from source, install
[Leiningen](http://leiningen.org), clone this repository, and run
`lein uberjar`. That will create `target/afterglow-max.jar` which is
the compiled code. Copy that into
`MaxPackage/afterglow-max/java-classes/lib`, and then copy
`MaxPackage/afterglow-max`, which is the Max package, to the Max
Packages folder as described in the last Installation step above.

> You can also create an alias of the `MaxPackage/afterglow-max`
> folder inside your Max Packages folder to avoid having to copy it
> from the repository every time you want to use an updated version in
> Max. The compiled jar file is set up to be ignored by git.

## Usage

The afterglow-max package provides a number of objects for monitoring
and controlling lighting cues. They are all based on the `mxj` object
since they are implemented in [Clojure](http://clojure.org/), like
afterglow itself, and rely on the Java virtual machine environment
that Max provides.

### mxj afterglow.max.Cue

Controls a single Afterglow
[Cue](https://github.com/brunchboy/afterglow/blob/master/doc/cues.adoc#cues),
based on its coordinates in the
[Cue Grid](https://github.com/brunchboy/afterglow/blob/master/doc/cues.adoc#the-cue-grid),
and sends updates about changes to its state. Lets you set initial
values for cue variables when the cue is triggerd by this object, and
adjust their values while the cue is running. Sends updates about
changes to those variables to its outlets while the cue is running,
whether or not the changes came from within Max.

![mxj afterglow.max.Cue](https://raw.githubusercontent.com/brunchboy/afterglow-max/master/doc/assets/Cue.png)

## License

<img align="right" alt="Deep Symmetry" src="doc/assets/DS-logo-bw-200-padded-left.png">
Copyright © 2015 [Deep Symmetry, LLC](http://deepsymmetry.org)

Distributed under the
[Eclipse Public License 1.0](http://opensource.org/licenses/eclipse-1.0.php),
the same as Clojure. By using this software in any fashion, you are
agreeing to be bound by the terms of this license. You must not remove
this notice, or any other, from this software. A copy of the license
can be found in
[resources/epl-v10.html](https://cdn.rawgit.com/brunchboy/afterglow-max/master/resources/epl-v10.html)
within this project.

