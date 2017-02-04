LOCAL_PATH := $(call my-dir)
LOCAL_MULTILIB := "both"
TARGET_PATH := $(LOCAL_PATH)/../node/out/Release/obj.target

# node

include $(CLEAR_VARS)
LOCAL_MODULE	:= node
LOCAL_SRC_FILES	:= $(TARGET_PATH)/libnode.a
include $(PREBUILT_STATIC_LIBRARY)

# deps

include $(CLEAR_VARS)
LOCAL_MODULE	:= uv
LOCAL_SRC_FILES	:= $(TARGET_PATH)/deps/uv/libuv.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= openssl
LOCAL_SRC_FILES	:= $(TARGET_PATH)/deps/openssl/libopenssl.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= http_parser
LOCAL_SRC_FILES	:= $(TARGET_PATH)/deps/http_parser/libhttp_parser.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= gtest
LOCAL_SRC_FILES	:= $(TARGET_PATH)/deps/gtest/libgtest.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= zlib
LOCAL_SRC_FILES := $(TARGET_PATH)/deps/zlib/libzlib.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= cares
LOCAL_SRC_FILES := $(TARGET_PATH)/deps/cares/libcares.a
include $(PREBUILT_STATIC_LIBRARY)

# intl

include $(CLEAR_VARS)
LOCAL_MODULE	:= icuucx
LOCAL_SRC_FILES	:= $(TARGET_PATH)/tools/icu/libicuucx.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= icui18n
LOCAL_SRC_FILES	:= $(TARGET_PATH)/tools/icu/libicui18n.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= icudata
LOCAL_SRC_FILES	:= $(TARGET_PATH)/tools/icu/libicudata.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= icustubdata
LOCAL_SRC_FILES := $(TARGET_PATH)/tools/icu/libicustubdata.a
include $(PREBUILT_STATIC_LIBRARY)


# v8

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_libbase
LOCAL_SRC_FILES	:= $(TARGET_PATH)/deps/v8/src/libv8_libbase.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_base
LOCAL_SRC_FILES := $(TARGET_PATH)/deps/v8/src/libv8_base.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_libplatform
LOCAL_SRC_FILES := $(TARGET_PATH)/deps/v8/src/libv8_libplatform.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_nosnapshot
LOCAL_SRC_FILES := $(TARGET_PATH)/deps/v8/src/libv8_nosnapshot.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VAR)
LOCAL_MODULE	:= v8_libsampler
LOCAL_SRC_FILES := $(TARGET_PATH)/deps/v8/src/libv8_libsampler.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_libstandalone_inspector
LOCAL_SRC_FILES	:= $(TARGET_PATH)/deps/v8_inspector/src/inspector/libstandalone_inspector.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := j2v8
TARGET_OUT		:= jniLibs/$(TARGET_ARCH_ABI)

LOCAL_SRC_FILES := com_eclipsesource_v8_V8Impl.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../node $(LOCAL_PATH)/../node/deps/v8 $(LOCAL_PATH)/../node/deps/v8/include $(LOCAL_PATH)/../node/deps/icu-small/source $(LOCAL_PATH)/../node/src
LOCAL_CFLAGS	+= -std=c++11 -Wall -Wno-unused-function -Wno-unused-variable -O3 -funroll-loops -ftree-vectorize -ffast-math -fpermissive -fPIC -D NODE_COMPATIBLE=1
LOCAL_LDLIBS	+= -L$(SYSROOT)/usr/lib -llog -latomic

LOCAL_WHOLE_STATIC_LIBRARIES := node
LOCAL_STATIC_LIBRARIES := \
	uv openssl http_parser gtest zlib cares \
	v8_base v8_nosnapshot v8_libplatform v8_libbase v8_libsampler v8_libstandalone_inspector \
	icui18n icuucx icudata icustubdata

include $(BUILD_SHARED_LIBRARY)
