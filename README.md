J2V8
====

[![Build Status](https://secure.travis-ci.org/eclipsesource/J2V8.png)](http://travis-ci.org/eclipsesource/J2V8)
[![Maven Central](https://img.shields.io/maven-central/v/com.eclipsesource.j2v8/j2v8_win32_x86.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.eclipsesource.j2v8%22)

J2V8 is a set of Java bindings for V8. J2V8 focuses on performance and tight integration with V8. It also takes a 'primitive first' approach, meaning that if a value can be accessed as a primitive, then it should be. This forces a more static type system between the JS and Java code, but it also improves the performance since intermediate Objects are not created.

Tutorials
==========
 * [Getting Started With J2V8](http://eclipsesource.com/blogs/getting-started-with-j2v8/)
 * [Registering Java Callbacks with J2V8](http://eclipsesource.com/blogs/2015/06/06/registering-java-callbacks-with-j2v8/)
 * [Implementing WebWorkers with J2V8](http://eclipsesource.com/blogs/2015/05/28/implementing-webworkers-with-j2v8/)
 * [Multithreaded JavaScript with J2V8](http://eclipsesource.com/blogs/2015/05/12/multithreaded-javascript-with-j2v8/)
 * [Using J2V8 with Heroku](http://eclipsesource.com/blogs/2015/06/04/using-j2v8-with-heroku/)

Articles
========
 * [Shipping J2V8 as an AAR](http://eclipsesource.com/blogs/2015/11/04/shipping-j2v8-as-an-aar/)
 * [Announcing J2V8 3.0](http://eclipsesource.com/blogs/2015/07/08/j2v8-3-0-released/)
 * [J2V8 2.2 New and Noteworthy](http://eclipsesource.com/blogs/2015/04/23/j2v8-2-2-new-and-noteworthy/)
 * [Announcing J2V8 2.0](http://eclipsesource.com/blogs/2015/02/25/announcing-j2v8-2-0/)
 * [Highly Efficient Java & JavaScript Integration](http://eclipsesource.com/blogs/2014/11/17/highly-efficient-java-javascript-integration/)

Presentations
=============
 * [J2V8 A Highly Efficient JS Runtime For Java](https://www.eclipsecon.org/na2015/session/j2v8-highly-efficient-js-runtime-java)
 * [Running Java Efficiently in a Java World](http://www.slideshare.net/irbull/enter-js)

Other Resources
===============
Here is a list of articles I've written on J2V8 [http://eclipsesource.com/blogs/tag/j2v8/](http://eclipsesource.com/blogs/tag/j2v8/).
 
Who is using J2V8?
========

Here are some projects that use J2V8:
* [tabris.js](https://tabrisjs.com)
* [tern.java](https://github.com/angelozerr/tern.java)


License
=====
The code is published under the terms of the [Eclipse Public License, version 1.0](http://www.eclipse.org/legal/epl-v10.html).
