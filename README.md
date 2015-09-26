# afterglow-max

[![Join the chat at https://gitter.im/brunchboy/afterglow](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/brunchboy/afterglow?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

A package for hosting
[Afterglow](https://github.com/brunchboy/afterglow#afterglow) inside
[Cycling ‘74’s Max](https://cycling74.com).

[![License](https://img.shields.io/github/license/brunchboy/afterglow-max.svg)](#license)

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
values for cue variables when the cue is started by this object, and
adjust their values while the cue is running regardless of how it was
started. Sends updates about changes to those variables to its outlets
while the cue is running, whether or not the changes came from within
Max.

![mxj afterglow.max.Cue](https://raw.githubusercontent.com/brunchboy/afterglow-max/master/doc/assets/Cue.png)

For more details, see the Help patcher, full Reference, and Examples
within Max.

### mxj afterglow.max.Eval

Evaluates Clojure expressions within the Afterglow context. Combined
with message objects, gives you an easy way to make Afterglow do
things that aren't worth creating special objects for. Useful examples
in the Help patcher include starting and opening the
[web interface](https://github.com/brunchboy/afterglow/blob/master/doc/README.adoc#the-embedded-web-interface),
enabling a connection from a full-featured Clojure development
environment for debugging custom effects, and activating an attached
Ableton Push controller.

![mxj afterglow.max.Eval](https://raw.githubusercontent.com/brunchboy/afterglow-max/master/doc/assets/Eval.png)

For more details, see the Help patcher and full Reference within Max.

### mxj afterglow.max.Metro

Adjust and query an Afterglow
[Metronome](https://github.com/brunchboy/afterglow/blob/master/doc/metronomes.adoc#metronomes),
allowing you to control and respond to the musical time that is
driving a light show.

![mxj afterglow.max.Var](https://raw.githubusercontent.com/brunchboy/afterglow-max/master/doc/assets/Metro.png)

For more details, see the Help patcher and full Reference within Max.

### mxj afterglow.max.NextFrame

Sends information about when Afterglow is next going to generate
control values for the lights, allowing patchers to set up appropriate
context, such as adjusting cue variables to be used in creating the
frame. Also can be used to start or stop the light show by sending
`start` and `stop` messages to its inlet.

![mxj afterglow.max.Var](https://raw.githubusercontent.com/brunchboy/afterglow-max/master/doc/assets/NextFrame.png)

For more details, see the Help patcher and full Reference within Max.

### mxj afterglow.max.Var

Sets and monitors the value of an Afterglow show variable.

![mxj afterglow.max.Var](https://raw.githubusercontent.com/brunchboy/afterglow-max/master/doc/assets/Var.png)

For more details, see the Help patcher and full Reference within Max.

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

   > :wrench: If you are using Max on Windows, see the
   > [Afterglow Wiki discussion](https://github.com/brunchboy/afterglow/wiki/Questions#ola-and-windows)
   > about OLA options.
   
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

3. Make sure you have an appropriate Java environment so that the
   `mxj` object in Max can work. Its reference page says &ldquo;Using the
   `mxj` object requires that the host computer have a current version
   of the Java Virtual Machine (JVM) installed.&rdquo;

    > :wrench: This statement is, however, a bit dated on the Mac.
    > Even the latest Max only supports the
    > [legacy Apple 1.6 JVM](http://support.apple.com/kb/dl1572),
    > which you need to install separately even if you have a current
    > Oracle JVM installed, as discussed on this
    > [Cycling ‘74 Wiki page](https://cycling74.com/wiki/index.php?title=Java_on_OS_X_10.9_(and_later)).

4. Download `afterglow-max.zip` from a <a
   href="https://github.com/brunchboy/afterglow-max/releases">release</a>,
   unzip it, and install the `afterglow-max` folder into Max's `Max
   7/Packages` folder (in your `Documents` folder). afterglow-max may
   also work with Max 6, or even Pure Data, but it hasn't been tested
   with them. If you try, please let us know how it goes on the
   [Wiki](https://github.com/brunchboy/afterglow-max/wiki)!

You should then be able to launch Max and experiment with the
afterglow-max objects! Once you want to start patching your own
fixtures and creating your own custom cues and effects within the
Clojure side of afterglow-max, you will want to edit the file
`afterglow-max/java-classes/init.clj`.

> :wrench: If you want to use an Ableton Push controller, and are
> using a Mac, you will need to work around some bugs in Java MIDI
> support on the Mac. The
> [Afterglow Wiki](https://github.com/brunchboy/afterglow/wiki/Questions#midi-from-java-on-the-mac)
> explains how to do that.

If you want to build from source, install
[Leiningen](http://leiningen.org), clone this repository, and run
`lein uberjar`. That will create `target/afterglow-max.jar` which is
the compiled code. Copy that into
`MaxPackage/afterglow-max/java-classes/lib`, and then copy
`MaxPackage/afterglow-max`, which is the Max package, to the Max
Packages folder as described in the last Installation step above.

> :bulb: You can also create an alias of the
> `MaxPackage/afterglow-max` folder inside your Max Packages folder to
> avoid having to copy it from the repository every time you want to
> use an updated version in Max. The compiled jar file is set up to be
> ignored by git. This makes working from source much more convenient.

## Troubleshooting

When afterglow-max has important events to report, or encounters
problems, it writes log entries to the normal Max console; that's the
first place to look when something seems not to be working right.
Following normal Max conventions, errors have a red background.

Since afterglow-max attempts things at frequent intervals, it
throttles its output to the Max console, and limits the number of
entries it creates there per minute. So if something is going wrong
frequently, you may miss log entries in the console. And that is not
the best place to try to read a Clojure stack trace even whent there
is only one to look at. You can find more complete logs in the
directory `afterglow-max/logs/` in the Max `Packages` directory where
you installed afterglow-max.

The Open Lighting Architecture's
[web interface](http://localhost:9090/new/#/), which you can find on
port 9090 of the machine running afterglow-max if you installed it in
the normal way, can be useful in troubleshooting as well. You can see
if the universes that afterglow-max is expecting to interact with
actually exist, are configured to talk to the lighting interfaces you
expect, and are sending DMX channel values that seem reasonable.

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

