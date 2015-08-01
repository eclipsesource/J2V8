#!/bin/bash

if [ -z "$V8_VERSION" ]; then echo "V8_VERSION not set"; exit 1;  fi
if [ -z "$J2V8_VERSION" ]; then echo "J2V8_VERSION not set"; exit 1;  fi

mkdir -p lib/

make clean
./configure --dest-cpu=x86
msbuild j2v8.sln /m /t:Build /p:Configuration=Release /clp:NoSummary;NoItemAndPropertyList;Verbosity=minimal /nologo
cp out/Release/lib.target/j2v8.ddl lib/libj2v8-$V8_VERSION-$J2V8_VERSION-windows-x86.dll

make clean
./configure --dest-cpu=x64
msbuild j2v8.sln /m /t:Build /p:Configuration=Release /clp:NoSummary;NoItemAndPropertyList;Verbosity=minimal /nologo
cp out/Release/lib.target/j2v8.ddl lib/libj2v8-$V8_VERSION-$J2V8_VERSION-windows-x64.dll
