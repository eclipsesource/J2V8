#!/bin/bash

V8_VERSION=$1
J2V8_VERSION=$2

mkdir -p lib/

./configure --dest-cpu=x86
make -j8
cp out/Release/lib.target/libj2v8.so lib/libj2v8-$V8_VERSION-$J2V8_VERSION-linux-x86.so

./configure --dest-cpu=x64
make -j8
cp out/Release/lib.target/libj2v8.so lib/libj2v8-$V8_VERSION-$J2V8_VERSION-linux-x64.so
