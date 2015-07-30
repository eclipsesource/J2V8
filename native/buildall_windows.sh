#!/bin/bash

./configure --dest-cpu=x86
msbuild j2v8.sln /m /t:Build /p:Configuration=Release /clp:NoSummary;NoItemAndPropertyList;Verbosity=minimal /nologo
