# How to contribute

Patches and third-party contributions are essential for keeping J2V8 great and
helping to evolve this library. There are a few guidelines that we need
contributors to follow so we have a chance to keep on top of things.

## Code contributions

Code contributions are greatly appreciated. We currently target V8 3.26, so
all contributions must link against that version of V8. Code contributions
should be accompanied by a set of unit tests. All tests are written in Java
and executed using JUnit. See `src/test/java` for examples.

## JNI layer

The C++ layer should be kept as thin as possible. This results in a more
verbose JNI layer, but it also moves the more complicated logic to Java.

## J2V8 core

J2V8 core contains the basic V8 bindings, represented as a hierarchy of
Java Classes. The core contains all the classes found in `com.eclipsesource.v8`.
J2V8 core should have no external dependencies other than JRE 1.6 and the JNI
layer.

## J2V8 Utils

J2V8 utils contains a higher level of abstraction and provides a number of
utilities for using J2V8 in a Java System. The J2V8 utils contains all
the classes found in `com.eclipsesource.v8.utils`. The J2V8 utils should
have no external dependencies other than JRE 1.6, and J2V8 core.

## Contributor License Agreement (CLA)

J2V8 was originally developed by Innoopract Informationssysteme GMBH for
the [Tabris.js](https://tabrisjs.com) project. However, we believe that
J2V8 is more broadly useful, which is why we Open Sourced the project.
To help protect all J2V8 users, and to protect all contributions, we are asking
that all contributors sign the CLA. A copy of the CLA can be found [here](https://www.clahub.com/agreements/eclipsesource/J2V8).

## Submitting changes

 * Sign the [Contributor License Agreement](https://www.clahub.com/agreements/eclipsesource/J2V8).
 * Create a GitHub issue describing to capture the issue.
 * Push your changes to a topic branch in your fork of the repository.
 * Submit a pull request to J2V8.
