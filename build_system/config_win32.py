import constants as c
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem

win32_config = PlatformConfig("win32", [c.arch_x86, c.arch_x64], DockerBuildSystem)

#-----------------------------------------------------------------------
def build_node_js(config, arch):
    return [
        "cd ./node",
        "vcbuild.bat release $ARCH",
    ]

win32_config.build_step(c.build_node_js, build_node_js)
#-----------------------------------------------------------------------
def build_j2v8_cmake(config, arch):
    cmake_gen_suffix = " Win64" if arch == c.arch_x64 else ""
    return [
        "python build_system/mkdir.py cmake.out/$PLATFORM.$ARCH",
        "cd cmake.out\\$PLATFORM.$ARCH",
        "rm -rf CMakeCache.txt CMakeFiles/",
        "cmake ..\\..\\ -G\"Visual Studio 14 2015" + cmake_gen_suffix + "\"",
    ]

win32_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
def build_j2v8_jni(config, arch):
    return [
        "cd cmake.out\$PLATFORM.$ARCH",
        "msbuild j2v8.sln /property:Configuration=Release",
    ]

win32_config.build_step(c.build_j2v8_jni, build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_java(config, arch):
    file_arch = "x86_64" if arch == c.arch_x64 else "x86"
    return [
        "set \"MVN_PLATFORM_NAME=win32\"",
        "set \"MVN_ARCH_NAME=" + file_arch + "\"",
        "mvn clean verify -e",
        # "mvn -DskipTests=true verify",
        "python build_system/mkdir.py build.out",
        "cp target/j2v8_win32_" + file_arch + "-4.7.0-SNAPSHOT.jar build.out/",
    ]

win32_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
