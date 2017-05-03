#!/bin/sh
set -e

DIR=`dirname $0`

set +e
rm -rf node.out
docker rm -f j2v8.android.x86
docker rm -f j2v8.android.arm
docker rm -f j2v8.linux.x64
set -e

tar xzf node.out-7_4_0.tar.gz

docker build -t "j2v8-linux-x64" -f docker/Dockerfile.linux $DIR
docker run -v $PWD:/build/. -v $PWD/node.out:/build/node --name j2v8.linux.x64 j2v8-linux-x64 

docker build -t "j2v8-android-x86" -f docker/Dockerfile.android $DIR
docker run -v $PWD/node.out:/build/node --name j2v8.android.x86 j2v8-android-x86 android-gcc-toolchain x86 --api 15 --host gcc-lpthread -C sh -c "cd jni && ndk-build" 
docker cp j2v8.android.x86:/build/jni/jniLibs $DIR/src/main/


