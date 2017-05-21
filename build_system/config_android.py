import constants as c
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem

android_config = PlatformConfig("android", [c.arch_x86, c.arch_arm], DockerBuildSystem)

android_config.cross_config(BuildConfig(
    name="cross-compile-host",
    platform="android",
    host_cwd="$CWD/docker/$PLATFORM",
    build_cwd="/j2v8",
))

#-----------------------------------------------------------------------
def build_node_js(config, arch):
    return [
        """android-gcc-toolchain $ARCH --api 17 --host gcc-lpthread -C \
            sh -c \"                \\
            cd ./node;              \\
            ./configure             \\
            --without-intl          \\
            --without-inspector     \\
            --dest-cpu=$ARCH        \\
            --dest-os=$PLATFORM     \\
            --without-snapshot      \\
            --enable-static &&      \\
            make -j4 > /dev/null\"  \\
            """,
    ]

android_config.build_step(c.build_node_js, build_node_js)
#-----------------------------------------------------------------------
def build_j2v8_cmake(config, arch):
    return [
        "mkdir -p cmake.out/$PLATFORM.$ARCH",
        "cd cmake.out/$PLATFORM.$ARCH",
        "rm -rf CMakeCache.txt CMakeFiles/",
        # "cmake -DCMAKE_TOOLCHAIN_FILE=/build/android-ndk-r13b/build/cmake/android.toolchain.cmake ../../",
        "cmake -DCMAKE_BUILD_TYPE=Release -DCMAKE_TOOLCHAIN_FILE=$BUILD_CWD/docker/android/android.$ARCH.toolchain.cmake ../../",
    ]

android_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
def build_j2v8_jni(config, arch):
    return [
        "cd cmake.out/$PLATFORM.$ARCH",
        "make -j4",
    ]

android_config.build_step(c.build_j2v8_jni, build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_java(config, arch):
    return [
        #TODO: add JDK & Maven to build jar
        "echo 'WARNING: no java build-step support yet'"
    ]

android_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
