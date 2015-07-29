-include config.mk

BUILDTYPE ?= Release
PYTHON ?= python
DESTDIR ?=
SIGN ?=
PREFIX ?= /usr/local

OSTYPE := $(shell uname -s | tr '[A-Z]' '[a-z]')

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
