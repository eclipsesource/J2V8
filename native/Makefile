-include config.mk

V8_VERSION = 4.2.77.21
J2V8_VERSION = SNAPSHOT

ifdef SystemRoot
	OSTYPE = windows
else
 	OSTYPE = $(shell uname -s | tr '[A-Z]' '[a-z]')
endif

BUILDTYPE ?= Release
PYTHON ?= python

J2V8 = j2v8
J2V8_G = j2v8_g

V ?= 1

ifeq ($(BUILDTYPE),Release)
all: out/Makefile $(J2V8)
else
all: out/Makefile $(J2V8) $(J2V8_G)
endif

.PHONY: $(J2V8) $(J2V8_G)

$(J2V8): config.gypi out/Makefile
	$(MAKE) -C out BUILDTYPE=Release V=$(V) CFLAGS="-fPIC" CXXFLAGS="-fPIC"

$(J2V8_G): config.gypi out/Makefile
	$(MAKE) -C out BUILDTYPE=Debug V=$(V) CFLAGS="-fPIC" CXXFLAGS="-fPIC"

out/Makefile: common.gypi deps/v8/build/toolchain.gypi deps/v8/build/features.gypi deps/v8/tools/gyp/v8.gyp j2v8.gyp config.gypi
	$(PYTHON) tools/gyp_j2v8.py -f make

config.gypi: configure
	if [ -f $@ ]; then
		$(error Stale $@, please re-run ./configure)
	else
		$(error No $@, please run ./configure first)
	fi

NDK_VERSION = r10e

include:
	cp -R deps/v8/include ./include/

ensurendk:
	if [ ! -d "android-ndk/" ]; then \
		wget -O android-ndk.bin http://dl.google.com/android/ndk/android-ndk-$(NDK_VERSION)-linux-x86_64.bin \
		chmod +x android-ndk.bin \
		./android-ndk.bin \
		rm android-ndk.bin \
		mv android-ndk-$(NDK_VERSION)/ android-ndk/; \
	fi

android: ensurendk
	$(eval TOOLCHAIN = $(shell pwd)/android-toolchain)
	mkdir -p ${TOOLCHAIN}
	android-ndk/build/tools/make-standalone-toolchain.sh \
	    --toolchain=arm-linux-androideabi-4.9 \
	    --arch=arm \
	    --install-dir=${TOOLCHAIN} \
	    --platform=android-9
	./configure \
	    --dest-cpu=arm \
	    --dest-os=android
	make -j8 \
		AR=${TOOLCHAIN}/bin/arm-linux-androideabi-ar \
		LINK=${TOOLCHAIN}/bin/arm-linux-androideabi-g++ \
		CC=${TOOLCHAIN}/bin/arm-linux-androideabi-gcc \
		CXX=${TOOLCHAIN}/bin/arm-linux-androideabi-g++
	mkdir -p lib/
	cp out/Release/lib.target/libj2v8.so lib/libj2v8-${V8_VERSION}-${J2V8_VERSION}-android-arm.so

x86:
	./configure --dest-cpu=x86
	make -j8
	mkdir -p lib/
	ifeq ($(OSTYPE), windows)
		cp out/Release/lib.target/libj2v8.so lib/j2v8-${V8_VERSION}-${J2V8_VERSION}-${OSTYPE}-x86.dll
	else
		cp out/Release/lib.target/libj2v8.so lib/libj2v8-${V8_VERSION}-${J2V8_VERSION}-${OSTYPE}-x86.so
	endif

x64:
	./configure --dest-cpu=x64
	make -j8
	mkdir -p lib/
	ifeq ($(OSTYPE), windows)
		cp out/Release/lib.target/libj2v8.so lib/j2v8-${V8_VERSION}-${J2V8_VERSION}-${OSTYPE}-x64.dll
	else
		cp out/Release/lib.target/libj2v8.so lib/libj2v8-${V8_VERSION}-${J2V8_VERSION}-${OSTYPE}-x64.so
	endif

clean:
	rm -Rf out/
