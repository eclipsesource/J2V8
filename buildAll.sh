#!/bin/bash
# 
# Builds J2V8 on Linux and Android
# usage: ./buildAll -r /path/to/root/of/project -s docker_image_suffix
#
# The path to the root of the project is optional, and if not specified
# the current directory will be used.
#

set -e
while [[ $# -gt 1 ]]
do
  key="$1"

  case $key in
      -r|--rootpath)
      ROOTPATH="$2"
      shift # past argument
      ;;
      -s|--suffix)
      DOCKER_CONTAINER_SUFFIX="$2"
      shift # past argument
      ;;
      *)
            # unknown option
      ;;
  esac
    shift # past argument or value
  done

if [ -z ${ROOTPATH} ];
then
  ROOTPATH=$PWD
fi

DIR=`dirname $0`

set +e
rm -rf node.out
docker rm -f j2v8.android.x86_$DOCKER_CONTAINER_SUFFIX
docker rm -f j2v8.android.arm_$DOCKER_CONTAINER_SUFFIX
docker rm -f j2v8.linux.x64_$DOCKER_CONTAINER_SUFFIX
set -e

tar xzf node.out-7_4_0.tar.gz

docker build -t "j2v8-linux-x64" -f docker/Dockerfile.linux $DIR
docker run -v $ROOTPATH:/build/. -v $ROOTPATH/node.out:/build/node --name j2v8.linux.x64_$DOCKER_CONTAINER_SUFFIX j2v8-linux-x64 

docker build -t "j2v8-android-x86" -f docker/Dockerfile.android $DIR
docker run -v $ROOTPATH/node.out:/build/node --name j2v8.android.x86_$DOCKER_CONTAINER_SUFFIX j2v8-android-x86 android-gcc-toolchain x86 --api 15 --host gcc-lpthread -C sh -c "cd jni && ndk-build && /build/android-ndk-r13b/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/arm-linux-androideabi/bin/strip --strip-unneeded -R .note -R .comment /build/jni/jniLibs/armeabi-v7a/libj2v8.so && strip --strip-unneeded -R .note -R .comment /build/jni/jniLibs/x86/libj2v8.so"
docker cp j2v8.android.x86_$DOCKER_CONTAINER_SUFFIX:/build/jni/jniLibs $DIR/src/main/

set +e
docker rm -f j2v8.android.x86_$DOCKER_CONTAINER_SUFFIX
docker rm -f j2v8.android.arm_$DOCKER_CONTAINER_SUFFIX
docker rm -f j2v8.linux.x64_$DOCKER_CONTAINER_SUFFIX
set -e
