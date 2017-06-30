
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

# adding cmake directory for includes
set(CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake)

# set up the module path
set(CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/cmake)

include(BuildUtils)
include(NodeJsUtils)
include(Policies)

#-----------------------------------------------------------------------
# BUILD PLATFORM SETUP & VARIABLES
#-----------------------------------------------------------------------

if(CMAKE_SYSTEM_NAME STREQUAL "Android")
#{
    set(JAVA_PLATFORM_NAME "android")

    # output library filename
    set(J2V8_LIB_PLATFORM_NAME "android")
    set(J2V8_LIB_PREFIX "")
    set(J2V8_LIB_ARCH_NAME ${CMAKE_ANDROID_ARCH_ABI})
#}
elseif(CMAKE_SYSTEM_NAME STREQUAL "Linux")
#{
    set(JAVA_PLATFORM_NAME "linux")

    # output library filename
    set(J2V8_LIB_PLATFORM_NAME "linux")
    set(J2V8_LIB_PREFIX "")
    set(J2V8_LIB_ARCH_NAME "x86")
#}
elseif(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
#{
    set(JAVA_PLATFORM_NAME "darwin")

    # output library filename
    set(J2V8_LIB_PLATFORM_NAME "macosx")
    set(J2V8_LIB_PREFIX "")
    set(J2V8_LIB_ARCH_NAME "x86")
#}
elseif(CMAKE_SYSTEM_NAME STREQUAL "Windows")
#{
    set(JAVA_PLATFORM_NAME "win32")

    # output library filename
    set(J2V8_LIB_PLATFORM_NAME "win32")
    set(J2V8_LIB_PREFIX "lib")
    set(J2V8_LIB_ARCH_NAME "x86")
#}
endif()

#-----------------------------------------------------------------------
# DEPENDENCY SETTINGS / CMAKE OPTIONS
#-----------------------------------------------------------------------

# look for dependencies
find_package(Java)

# j2v8 dependency options
set(J2V8_JDK_DIR ${Java_ROOT} CACHE STRING "Path to the Java JDK dependency")
set(J2V8_NODEJS_DIR "${CMAKE_SOURCE_DIR}/node" CACHE STRING "Path to the Node.js dependency")

# get the required Node.js link libraries
get_njs_libs(${J2V8_NODEJS_DIR} "Debug")
get_njs_libs(${J2V8_NODEJS_DIR} "Release")

# j2v8 build options
option(J2V8_NODE_COMPATIBLE "Build the J2V8 native bridge with Node.js support enabled" ON)
option(J2V8_BUILD_ONLY_DEBUG_RELEASE "Generate only Debug and Release configurations (exclude RelWithDebInfo and MinSizeRel)" ON)

if(CMAKE_SYSTEM_NAME STREQUAL "Windows" AND MSVC)
#{
    option(J2V8_LINK_WITH_STATIC_MSVCRT "Link against the static version of the Microsoft Visual C++ Common Runtime (will link against the dynamic DLL version if this option is disabled)" ON)
#}
endif()

#-----------------------------------------------------------------------
# INCLUDE DIRECTORIES & SOURCE FILES
#-----------------------------------------------------------------------

# project include directories
set(include_dirs
    ${J2V8_NODEJS_DIR}
    ${J2V8_NODEJS_DIR}/src
    ${J2V8_NODEJS_DIR}/deps/v8
    ${J2V8_NODEJS_DIR}/deps/v8/include
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

# tell gcc/clang to use the c++11 standard
set(CMAKE_CXX_STANDARD 11)

if(CMAKE_SYSTEM_NAME STREQUAL "Android")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -Wno-unused-function -Wno-unused-variable -O3 -funroll-loops -ftree-vectorize -ffast-math -fpermissive -fPIC ")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wno-unused-function -Wno-unused-variable -O3 -funroll-loops -ftree-vectorize -ffast-math -fpermissive -fPIC ")
endif()

# remove the MinSizeRel and RelWithDebInfo configurations
if(J2V8_BUILD_ONLY_DEBUG_RELEASE)
#{
    set(CMAKE_CONFIGURATION_TYPES "Debug;Release" CACHE STRING "limited configs" FORCE)
#}
endif()

# link against the static MS C++ runtime libraries
if(J2V8_LINK_WITH_STATIC_MSVCRT)
    link_static_crt()
endif()

# create the j2v8 library
add_library(j2v8 SHARED ${src_files})

# enable Node.js if requested by the build options above
if(J2V8_NODE_COMPATIBLE)
#{
    set_property(TARGET j2v8 PROPERTY COMPILE_DEFINITIONS ${COMPILE_DEFINITIONS} NODE_COMPATIBLE=1)
#}
endif()

# build output directory
set(LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/lib)

# set the include directories
include_directories(${include_dirs})

# link the necessary libraries
target_link_libraries(j2v8
    debug "${njs_Debug_libs}"
    optimized "${njs_Release_libs}"
)

#-----------------------------------------------------------------------
# OUTPUT SETTINGS & POST-BUILD
#-----------------------------------------------------------------------

# apply lib suffix if building a 64 bit target
if(CMAKE_CL_64 OR CMAKE_SIZEOF_VOID_P EQUAL 8)
    set(J2V8_LIB_ARCH_NAME "${J2V8_LIB_ARCH_NAME}_64")
endif()

# set library output filename
set_target_properties(j2v8 PROPERTIES OUTPUT_NAME "${J2V8_LIB_PREFIX}${PROJECT_NAME}_${J2V8_LIB_PLATFORM_NAME}_${J2V8_LIB_ARCH_NAME}")
