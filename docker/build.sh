#!/bin/sh

DIR=`dirname $0`

cp -R $DIR/../jni $DIR

docker rm -v nodebuild
docker build -t "node-build" $DIR
docker run --name nodebuild node-build
docker cp nodebuild:/build/jni/jniLibs $DIR/../src/main/
