cmake_minimum_required (VERSION 2.6)
project (j2v8)

# adding cmake directory for includes
set(CMAKE_MODULE_PATH ${PROJECT_SOURCE_DIR}/cmake)

include (BuildUtils)
include (NodeJsUtils)
include (Policies)

#-----------------------------------------------------------------------
# CMAKE OPTIONS
#-----------------------------------------------------------------------
option(J2V8_NODE_COMPATIBLE "Build the J2V8 native bridge with Node.js support enabled" ON)
option(J2V8_BUILD_ONLY_DEBUG_RELEASE "Generate only Debug and Release configurations (exclude RelWithDebInfo and MinSizeRel)" ON)

# set up the module path
set (CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR}/cmake)

# remove the MinSizeRel and RelWithDebInfo configurations
if (J2V8_BUILD_ONLY_DEBUG_RELEASE)
#{
    set (CMAKE_CONFIGURATION_TYPES "Debug;Release" CACHE STRING "limited configs" FORCE)
#}
endif ()

#-----------------------------------------------------------------------
# DEPENDENCY SETTINGS & DISCOVERY
#-----------------------------------------------------------------------

# look for dependencies
find_package(Java)

# overridable settings
set (J2V8_JDK_DIR ${Java_ROOT} CACHE STRING "Path to the Java JDK dependency")
set (J2V8_NODEJS_DIR "./node" CACHE STRING "Path to the Node.js dependency")

# get lists of the required Node.js link libraries
get_njs_libs (${J2V8_NODEJS_DIR} "Debug")
get_njs_libs (${J2V8_NODEJS_DIR} "Release")

#-----------------------------------------------------------------------
# INCLUDE DIRECTORIES & SOURCE FILES
#-----------------------------------------------------------------------

# project include directories
set (include_dirs
    ${J2V8_JDK_DIR}/include
    ${J2V8_JDK_DIR}/include/win32
    ${J2V8_NODEJS_DIR}
    ${J2V8_NODEJS_DIR}/src
    ${J2V8_NODEJS_DIR}/deps/v8
    ${J2V8_NODEJS_DIR}/deps/v8/include
)

# project source files
set (src_files
    jni/com_eclipsesource_v8_V8Impl.cpp
    jni/com_eclipsesource_v8_V8Impl.h
)

source_group("" FILES ${src_files})

#-----------------------------------------------------------------------
# BUILD SETTINGS & COMPILATION
#-----------------------------------------------------------------------

link_static_crt()

# create the j2v8 library
add_library (j2v8 SHARED ${src_files})

# enable Node.js if requested by the set options
if (J2V8_NODE_COMPATIBLE)
#{
    set_property (TARGET j2v8 PROPERTY COMPILE_DEFINITIONS ${COMPILE_DEFINITIONS} NODE_COMPATIBLE=1)
#}
endif ()

# build output directory
set (LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/lib)

# set the include directories
include_directories (${include_dirs})

# link the necessary libraries
target_link_libraries (j2v8
    debug "${njs_Debug_libs}"
    optimized "${njs_Release_libs}"
)

#-----------------------------------------------------------------------
# OUTPUT SETTINGS & POST-BUILD
#-----------------------------------------------------------------------

if (CMAKE_CL_64)
    set (ARCH_SUFFIX "_64")
endif ()

set_target_properties (j2v8 PROPERTIES OUTPUT_NAME "${PROJECT_NAME}_win32_x86${ARCH_SUFFIX}")

# copy native lib to Java project resources directory
add_custom_command (TARGET j2v8 POST_BUILD
                    COMMAND ${CMAKE_COMMAND} -E copy_if_different
                    $<TARGET_FILE:j2v8>
                    ${CMAKE_SOURCE_DIR}/src/main/resources/lib$<TARGET_FILE_NAME:j2v8>
)
