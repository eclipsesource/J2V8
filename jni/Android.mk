LOCAL_PATH := $(call my-dir)
LOCAL_MULTILIB := "both"
TARGET_PATH := $(LOCAL_PATH)/../node/node.$(TARGET_ARCH_ABI)/out/Release/obj.target/deps/v8/src

# v8

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_libbase
LOCAL_SRC_FILES	:= $(TARGET_PATH)/libv8_libbase.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_base
LOCAL_SRC_FILES := $(TARGET_PATH)/libv8_base.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_libplatform
LOCAL_SRC_FILES := $(TARGET_PATH)/libv8_libplatform.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_nosnapshot
LOCAL_SRC_FILES := $(TARGET_PATH)/libv8_nosnapshot.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VAR)
LOCAL_MODULE	:= v8_libsampler
LOCAL_SRC_FILES := $(TARGET_PATH)/libv8_libsampler.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := j2v8
TARGET_OUT		:= jniLibs/$(TARGET_ARCH_ABI)

LOCAL_SRC_FILES := com_eclipsesource_v8_V8Impl.cpp
LOCAL_C_INCLUDES := $(LOCAL_PATH)/../node/node.$(TARGET_ARCH_ABI)/deps $(LOCAL_PATH)/../node/node.$(TARGET_ARCH_ABI)/deps/v8 $(LOCAL_PATH)/../node/node.$(TARGET_ARCH_ABI)/deps/v8/include $(LOCAL_PATH)/../node/node.$(TARGET_ARCH_ABI)/deps/icu-small/source 
LOCAL_CFLAGS	+= -std=c++11 -Wall -Wno-unused-function -Wno-unused-variable -O3 -funroll-loops -ftree-vectorize -ffast-math -fpermissive -fPIC 
LOCAL_LDLIBS	+= -L$(SYSROOT)/usr/lib -llog -latomic

LOCAL_STATIC_LIBRARIES := \
	v8_base v8_nosnapshot v8_libplatform v8_libbase v8_libsampler

include $(BUILD_SHARED_LIBRARY)
