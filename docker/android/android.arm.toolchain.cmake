set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_SYSTEM_VERSION 21) # API level

set(CMAKE_ANDROID_ARCH arm)
set(CMAKE_ANDROID_ARCH_ABI armeabi-v7a)
set(CMAKE_ANDROID_NDK /build/android-ndk-r14b/)
set(CMAKE_ANDROID_STL_TYPE gnustl_static)

# ARM specific settings
set(CMAKE_ANDROID_ARM_NEON 1)
