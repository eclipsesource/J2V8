# Android NDK r26d Toolchain Configuration for ARM
# This file delegates to Android NDK's official CMake toolchain

set(ANDROID_ABI armeabi-v7a)
set(ANDROID_PLATFORM android-21)
set(ANDROID_STL c++_static)
set(ANDROID_ARM_NEON ON)

# Include Android NDK's official toolchain file
include(/build/android-ndk-r26d/build/cmake/android.toolchain.cmake)
