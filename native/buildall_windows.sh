#!/bin/bash

V8_VERSION=$1
J2V8_VERSION=$2

mkdir -p lib/

./configure --dest-cpu=x86
msbuild j2v8.sln /m /t:Build /p:Configuration=Release /clp:NoSummary;NoItemAndPropertyList;Verbosity=minimal /nologo
cp out/Release/lib.target/j2v8.ddl lib/libj2v8-$V8_VERSION-$J2V8_VERSION-windows-x86.dll

./configure --dest-cpu=x64
msbuild j2v8.sln /m /t:Build /p:Configuration=Release /clp:NoSummary;NoItemAndPropertyList;Verbosity=minimal /nologo
cp out/Release/lib.target/j2v8.ddl lib/libj2v8-$V8_VERSION-$J2V8_VERSION-windows-x64.dll
