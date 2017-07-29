J2V8
====

[![Build Status](https://secure.travis-ci.org/eclipsesource/J2V8.png)](http://travis-ci.org/eclipsesource/J2V8)
[![Maven Central](https://img.shields.io/maven-central/v/com.eclipsesource.j2v8/j2v8_win32_x86.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.eclipsesource.j2v8%22)

J2V8 is a set of Java bindings for V8. J2V8 focuses on performance and tight integration with V8. It also takes a 'primitive first' approach, meaning that if a value can be accessed as a primitive, then it should be. This forces a more static type system between the JS and Java code, but it also improves the performance since intermediate Objects are not created.

We developed J2V8 as a high performance engine for our multi-platform mobile toolkit [tabris.js](https://tabrisjs.com) and it is a great choice for executing JavaScript on Android devices.

Building J2V8
=============
Building J2V8 requires building both the native parts and the Java library (.jar/.aar file). To build the native parts we first build node.js as a library and then statically link J2V8 to that. The Java parts are built with maven/gradle.

J2V8 uses a cross-platform, cross-compiling build-system written in Python.

Follow these steps to build J2V8 from source:

1) clone the Node.js source code
    - `python prepare_build.py`
    - This will download & prepare the latest compatible Node.js version for use in J2V8
    - The Node.js source code will be cloned into the local `node` sub-directory, which is the expected default location for the J2V8 build
2) build Node.js and the J2V8 library
    - `python build.py --target linux --arch x64 --node-enabled --cross-compile`
    - or shorthand
    - `python build.py -t linux -a x64 -ne -x`

For all available options, supported platforms and architectures you can consult the build-script help:

`python build.py --help`

Cross-Compiling
---------------

For cross-compiling J2V8 uses [Docker](https://www.docker.com/) (android, linux, windows) and [Vagrant](https://www.vagrantup.com/) (macos).
The full source-code (of both J2V8 and Node.js) on the build-host are just shared via mounted volumes with the Docker / Vagrant machines, so you can quickly make changes and perform builds fast.

To invoke a cross-compile build, simply invoke the `build.py` script as usual but add the `--cross-compile`, `-x` flag.
This will automatically provision and run the necessary virtualization to run the requested build fully independent of your local environment.

<b>Note:</b> using Docker / Vagrant for cross-compiliation requires many gigabytes of harddrive space as well as downloading the required images & tools.

Tutorials
==========
 * [Getting Started With J2V8](https://eclipsesource.com/blogs/tutorials/getting-started-with-j2v8/)
 * [Registering Java Callbacks with J2V8](http://eclipsesource.com/blogs/2015/06/06/registering-java-callbacks-with-j2v8/)
 * [Implementing WebWorkers with J2V8](http://eclipsesource.com/blogs/2015/05/28/implementing-webworkers-with-j2v8/)
 * [Multithreaded JavaScript with J2V8](http://eclipsesource.com/blogs/2015/05/12/multithreaded-javascript-with-j2v8/)
 * [Using J2V8 with Heroku](http://eclipsesource.com/blogs/2015/06/04/using-j2v8-with-heroku/)

Articles
========
 * [Announcing J2V8 4](http://eclipsesource.com/blogs/2016/07/20/announcing-j2v8-4/)
 * [Running Node.js on the JVM](http://eclipsesource.com/blogs/2016/07/20/running-node-js-on-the-jvm/)
 * [Shipping J2V8 as an AAR](http://eclipsesource.com/blogs/2015/11/04/shipping-j2v8-as-an-aar/)
 * [Announcing J2V8 3.0](http://eclipsesource.com/blogs/2015/07/08/j2v8-3-0-released/)
 * [J2V8 2.2 New and Noteworthy](http://eclipsesource.com/blogs/2015/04/23/j2v8-2-2-new-and-noteworthy/)
 * [Announcing J2V8 2.0](http://eclipsesource.com/blogs/2015/02/25/announcing-j2v8-2-0/)
 * [Highly Efficient Java & JavaScript Integration](http://eclipsesource.com/blogs/2014/11/17/highly-efficient-java-javascript-integration/)

Presentations
=============
 * [J2V8 A Highly Efficient JS Runtime For Java](https://www.eclipsecon.org/na2015/session/j2v8-highly-efficient-js-runtime-java)
 * [Running JavaScript Efficiently in a Java World](http://www.slideshare.net/irbull/enter-js)

Other Resources
===============
Here is a list of articles I've written on J2V8 [http://eclipsesource.com/blogs/tag/j2v8/](http://eclipsesource.com/blogs/tag/j2v8/).

Who is using J2V8?
========

Here are some projects that use J2V8:
* [tabris.js](https://tabrisjs.com)
* [tern.java](https://github.com/angelozerr/tern.java)
* [PlantUML](http://plantuml.com/)
* [jooby](http://jooby.org/doc/assets)
* [Alicorn](http://alicorn.io)

License
=====
The code is published under the terms of the [Eclipse Public License, version 1.0](http://www.eclipse.org/legal/epl-v10.html).
