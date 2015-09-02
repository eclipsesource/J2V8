#!/bin/bash
# for android: apt-get install libc6-dev-i386 g++-multilib

if [ -z "$V8_VERSION" ]; then echo "V8_VERSION not set"; exit 1;  fi
if [ -z "$J2V8_VERSION" ]; then echo "J2V8_VERSION not set"; exit 1;  fi

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

function build {
	make clean
	rm -Rf $TOOLCHAIN
	mkdir -p $TOOLCHAIN
	android-ndk/build/tools/make-standalone-toolchain.sh \
	    --toolchain=$3 \
	    --arch=$1 \
	    --install-dir=$TOOLCHAIN \
	    --platform=$2
	./configure \
	    --dest-cpu=$5 \
	    --dest-os=android
	make -j8 \
		AR=$TOOLCHAIN/bin/$4-ar \
		LINK=$TOOLCHAIN/bin/$4-g++ \
		CC=$TOOLCHAIN/bin/$4-gcc \
		CXX=$TOOLCHAIN/bin/$4-g++
	mkdir -p lib/
	cp out/Release/lib.target/libj2v8.so lib/libj2v8-$V8_VERSION-$J2V8_VERSION-android-$5.so
}

build arm android-9 arm-linux-androideabi-4.9 arm-linux-androideabi arm
build arm64 android-21 aarch64-linux-android-4.9 aarch64-linux-android arm64

#build mips android-9 mipsel-linux-android-4.9 mipsel-linux-android mips

build x86 android-9 x86-4.9 i686-linux-android x86
build x86_64 android-21 x86_64-4.9 x86_64-linux-android x64
