# NOTE: this works around the windows docker / msbuild bug, that causes the
# VC++ linker to fail creating PDB debug files in a shared volume
# (see: https://social.msdn.microsoft.com/Forums/en-US/bd281c61-8e04-4f56-aec2-5ad565271d7f/windowsservercorelatest-vc-2015-build-tools-throws-linker-error-lnk1318-unexpected-pdb?forum=windowscontainers)
if(J2V8_CROSS_COMPILE AND J2V8_WIN32_PDB_DOCKER_FIX)
#{
    # TODO: remove this if the Win32 Docker PDB bug is fixed
    # see: https://stackoverflow.com/a/39109288
    message("CMAKE-WARNING: applied workaround for windows docker PDB bug")
    message("see: https://social.msdn.microsoft.com/Forums/en-US/bd281c61-8e04-4f56-aec2-5ad565271d7f/windowsservercorelatest-vc-2015-build-tools-throws-linker-error-lnk1318-unexpected-pdb?forum=windowscontainers")
    set(CMAKE_TRY_COMPILE_TARGET_TYPE STATIC_LIBRARY)
#}
endif()

cmake_minimum_required(VERSION 3.6)
project(j2v8)
set(CMAKE_VERBOSE_MAKEFILE ON)

# set up the module path
set(CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake)

include(BuildUtils)
include(Policies)

#-----------------------------------------------------------------------
# DEPENDENCY SETTINGS / CMAKE OPTIONS
#-----------------------------------------------------------------------

# look for dependencies
find_package(Java)

# j2v8 dependency options
set(J2V8_JDK_DIR ${Java_ROOT} CACHE STRING "Path to the Java JDK dependency")
set(J2V8_V8_DIR "${CMAKE_SOURCE_DIR}/v8.out" CACHE STRING "Path to the V8 JS engine dependency")

# j2v8 build options
set(J2V8_TARGET_ARCH "" CACHE STRING "The target architecture for the build.")
option(J2V8_BUILD_ONLY_DEBUG_RELEASE "Generate only Debug and Release configurations (exclude RelWithDebInfo and MinSizeRel)" ON)

if(CMAKE_SYSTEM_NAME STREQUAL "Windows" AND MSVC)
    option(J2V8_LINK_WITH_STATIC_MSVCRT "Link against the static version of the Microsoft Visual C++ Common Runtime (will link against the dynamic DLL version if this option is disabled)" ON)
endif()

#-----------------------------------------------------------------------
# BUILD PLATFORM SETUP & VARIABLES
#-----------------------------------------------------------------------

# HINT: CMake Multiarchitecture Compilation
# see: https://stackoverflow.com/a/5359572/425532

if("${J2V8_TARGET_ARCH}" STREQUAL "")
    message (FATAL_ERROR "J2V8_TARGET_ARCH not specified")
endif()

if(J2V8_TARGET_ARCH STREQUAL "x86_64")
    set(J2V8_BUILD_X64 TRUE)
endif()

if(CMAKE_SYSTEM_NAME STREQUAL "Android")
#{
    set(JAVA_PLATFORM_NAME "android")

    # output library filename parts
    set(J2V8_LIB_PREFIX "")
    set(J2V8_LIB_ARCH_NAME ${CMAKE_ANDROID_ARCH_ABI})
    set(J2V8_LIB_VENDOR_NAME "")
    set(J2V8_LIB_PLATFORM_NAME "android")
#}
elseif(CMAKE_SYSTEM_NAME STREQUAL "Linux")
#{
    set(JAVA_PLATFORM_NAME "linux")

    # output library filename parts
    set(J2V8_LIB_PREFIX "")
    set(J2V8_LIB_ARCH_NAME ${J2V8_TARGET_ARCH})
    set(J2V8_LIB_VENDOR_NAME "")
    set(J2V8_LIB_PLATFORM_NAME "linux")

    if(J2V8_VENDOR)
        set(J2V8_LIB_VENDOR_NAME "-${J2V8_VENDOR}")
    endif()

    # configure library architecture
    if(J2V8_BUILD_X64)
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -m64 ")
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -m64 ")
    else()
        set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -m32 ")
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -m32 ")
    endif()

    # -lrt ... see: https://github.com/eclipsesource/J2V8/issues/292
    set (j2v8_Debug_libs "-lrt")
    set (j2v8_Release_libs"-lrt")
#}
elseif(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
#{
    set(JAVA_PLATFORM_NAME "darwin")

    # output library filename parts
    set(J2V8_LIB_PREFIX "")
    set(J2V8_LIB_ARCH_NAME ${J2V8_TARGET_ARCH})
    set(J2V8_LIB_VENDOR_NAME "")
    set(J2V8_LIB_PLATFORM_NAME "macosx")

    # configure library architecture
    if(J2V8_BUILD_X64)
        set(CMAKE_OSX_ARCHITECTURES "x86_64")
    else()
        set(CMAKE_OSX_ARCHITECTURES "i386")

        # fix for 32-bit linking error "ld: illegal text reloc"
        # see: https://stackoverflow.com/a/9322458/425532
        set(CMAKE_SHARED_LINKER_FLAGS "-read_only_relocs suppress")
    endif()
#}
elseif(CMAKE_SYSTEM_NAME STREQUAL "Windows")
#{
    set(JAVA_PLATFORM_NAME "win32")

    # output library filename parts
    set(J2V8_LIB_PREFIX "lib")
    set(J2V8_LIB_ARCH_NAME ${J2V8_TARGET_ARCH})
    set(J2V8_LIB_VENDOR_NAME "")
    set(J2V8_LIB_PLATFORM_NAME "windows")
#}
endif()

message("--------------------------------------------------")
message("J2V8_LIB_ARCH_NAME     = ${J2V8_LIB_ARCH_NAME}")
message("J2V8_LIB_VENDOR_NAME   = ${J2V8_LIB_VENDOR_NAME}")
message("J2V8_LIB_PLATFORM_NAME = ${J2V8_LIB_PLATFORM_NAME}")
message("J2V8_TARGET_ARCH       = ${J2V8_TARGET_ARCH}")
message("J2V8_BUILD_X64         = ${J2V8_BUILD_X64}")
message("--------------------------------------------------")

#-----------------------------------------------------------------------
# INCLUDE DIRECTORIES & SOURCE FILES
#-----------------------------------------------------------------------

# project include directories
set(include_dirs
    ${J2V8_V8_DIR}/include
)

# do not use JAVA_HOME for Java include files for Android
# (instead the Android NDK toolchain will introduce them automatically)
if(NOT CMAKE_SYSTEM_NAME STREQUAL "Android")
#{
    set(include_dirs
        ${include_dirs}
        ${J2V8_JDK_DIR}/include
        ${J2V8_JDK_DIR}/include/${JAVA_PLATFORM_NAME}
    )
#}
endif()

# project source files
set(src_files
    jni/com_eclipsesource_v8_V8Impl.cpp
    jni/com_eclipsesource_v8_V8Impl.h
)

source_group("" FILES ${src_files})

#-----------------------------------------------------------------------
# BUILD SETTINGS & COMPILATION
#-----------------------------------------------------------------------

# tell gcc/clang to use the c++17 standard
set(CMAKE_CXX_STANDARD 17)

if(CMAKE_SYSTEM_NAME STREQUAL "Android")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wno-unused-function -Wno-unused-variable -O3 -funroll-loops -ftree-vectorize -ffast-math -fpermissive -fPIC ")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wno-unused-function -Wno-unused-variable -O3 -funroll-loops -ftree-vectorize -ffast-math -fpermissive -fPIC ")
endif()

# remove the MinSizeRel and RelWithDebInfo configurations
if(J2V8_BUILD_ONLY_DEBUG_RELEASE)
    set(CMAKE_CONFIGURATION_TYPES "Debug;Release" CACHE STRING "limited configs" FORCE)
endif()

# link against the static MS C++ runtime libraries
if(J2V8_LINK_WITH_STATIC_MSVCRT)
    link_static_crt()
endif()

# create the j2v8 library
add_library(v8_monolith STATIC IMPORTED)
set_target_properties(v8_monolith PROPERTIES IMPORTED_LOCATION ${J2V8_V8_DIR}/${J2V8_MONOLITH_LIB_DIR}/libv8_monolith.a)

add_library(j2v8 SHARED ${src_files})

if(CMAKE_SYSTEM_NAME STREQUAL "Windows" AND MSVC)
    set_property(TARGET j2v8 APPEND_STRING PROPERTY LINK_FLAGS_RELEASE "/LTCG")
endif()

# build output directory
set(LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/lib)

# set the include directories
include_directories(${include_dirs})

# link the necessary libraries
target_link_libraries(j2v8
    v8_monolith
    debug "${j2v8_Debug_libs}"
    optimized "${j2v8_Release_libs}"
    "${libgcc}"
)

#-----------------------------------------------------------------------
# OUTPUT SETTINGS & POST-BUILD
#-----------------------------------------------------------------------

# set library output filename
set_target_properties(j2v8 PROPERTIES OUTPUT_NAME "${J2V8_LIB_PREFIX}${PROJECT_NAME}${J2V8_LIB_VENDOR_NAME}-${J2V8_LIB_PLATFORM_NAME}-${J2V8_LIB_ARCH_NAME}")
