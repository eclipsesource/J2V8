LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_base
#LOCAL_SRC_FILES := ../support/android/libs/libv8_base.arm.a
#LOCAL_SRC_FILES := ../libv8_base.arm.a
LOCAL_SRC_FILES := /data/jenkins/v8_3_26/out/android_arm.release/obj.target/tools/gyp/libv8_base.arm.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE	:= v8_nosnapshot
#LOCAL_SRC_FILES :=  ../support/android/libs/libv8_nosnapshot.arm.a
#LOCAL_SRC_FILES :=  ../libv8_nosnapshot.arm.a
LOCAL_SRC_FILES :=  /data/jenkins/v8_3_26/out/android_arm.release/obj.target/tools/gyp/libv8_nosnapshot.arm.a
include $(PREBUILT_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE	:= libicuuc
#LOCAL_SRC_FILES :=  ../support/android/libs/libicuuc.a
#LOCAL_SRC_FILES :=  ../libicuuc.a
#LOCAL_SRC_FILES :=  /data/jenkins/v8_3_26/out/android_arm.release/obj.target/tools/gyp/libicuuc.a
#include $(PREBUILT_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE	:= libicui18n
#LOCAL_SRC_FILES :=  ../support/android/libs/libicui18n.a
#LOCAL_SRC_FILES :=  ../libicui18n.a
#include $(PREBUILT_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE	:= libicudata
#LOCAL_SRC_FILES :=  ../support/android/libs/libicudata.a
#LOCAL_SRC_FILES :=  ../libicudata.a
#include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := j2v8_android_armv7l
LOCAL_SRC_FILES := com_eclipsesource_v8_V8Impl.cpp 
LOCAL_C_INCLUDES := /data/jenkins/v8_3_26/include/
LOCAL_CFLAGS += -Wall -Wno-unused-function -Wno-unused-variable -O3 -funroll-loops -ftree-vectorize -ffast-math -fpermissive
#LOCAL_STATIC_LIBRARIES := v8_base v8_nosnapshot libicui18n libicuuc libicudata
LOCAL_STATIC_LIBRARIES := v8_base v8_nosnapshot
LOCAL_LDLIBS += -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)
