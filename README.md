J2V8
====

J2V8 is a set of V8 bindings for Java. J2V8 focuses on performance and tight integration with V8. It also takes a 'primitive first' approach, meaning that if a value can be accessed as a primitive, then it should be. This forces a more static type system between the JS and Java code, but it also improves the performance since intermediate Objects are not created.

The Builds
==========

Builds of J2V8 are available at [https://build.eclipsesource.com/tabris/jenkins/job/J2V8/]. Currently only Android and Linux builds are available. Both the jar and the platform dependent .so file is needed. 
