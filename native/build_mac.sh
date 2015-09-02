#!/bin/bash

if [ -z "$V8_VERSION" ]; then echo "V8_VERSION not set"; exit 1;  fi
if [ -z "$J2V8_VERSION" ]; then echo "J2V8_VERSION not set"; exit 1;  fi

mkdir -p lib/

make clean
./configure --dest-cpu=x86
make -j8
cp out/Release/libj2v8.dylib lib/libj2v8-$V8_VERSION-$J2V8_VERSION-mac-x86.dylib

make clean
./configure --dest-cpu=x64
make -j8
cp out/Release/libj2v8.dylib lib/libj2v8-$V8_VERSION-$J2V8_VERSION-mac-x64.dylib
