# Android NDK r26d Toolchain Configuration for x86
# This file delegates to Android NDK's official CMake toolchain

set(ANDROID_ABI x86)
set(ANDROID_PLATFORM android-21)
set(ANDROID_STL c++_static)

# Include Android NDK's official toolchain file
include(/build/android-ndk-r26d/build/cmake/android.toolchain.cmake)
