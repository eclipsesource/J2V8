#!/bin/bash
# for android: apt-get install libc6-dev-i386 g++-multilib

V8_VERSION=$1
J2V8_VERSION=$2

NDK_VERSION=r10e

mkdir -p lib/

if [ ! -d "android-ndk/" ]; then
	wget -O android-ndk.bin http://dl.google.com/android/ndk/android-ndk-$NDK_VERSION-linux-x86_64.bin
	chmod +x android-ndk.bin
	./android-ndk.bin
	rm android-ndk.bin
	mv android-ndk-$NDK_VERSION/ android-ndk/;
fi

TOOLCHAIN=`pwd`/android-toolchain
mkdir -p $TOOLCHAIN
android-ndk/build/tools/make-standalone-toolchain.sh \
    --toolchain=arm-linux-androideabi-4.9 \
    --arch=arm \
    --install-dir=$TOOLCHAIN \
    --platform=android-9
./configure \
    --dest-cpu=arm \
    --dest-os=android
make -j8 \
	AR=$TOOLCHAIN/bin/arm-linux-androideabi-ar \
	LINK=$TOOLCHAIN/bin/arm-linux-androideabi-g++ \
	CC=$TOOLCHAIN/bin/arm-linux-androideabi-gcc \
	CXX=$TOOLCHAIN/bin/arm-linux-androideabi-g++
mkdir -p lib/
cp out/Release/lib.target/libj2v8.so lib/libj2v8-$V8_VERSION-$J2V8_VERSION-android-arm.so
