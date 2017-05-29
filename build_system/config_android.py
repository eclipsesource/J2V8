import constants as c
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem
from build_utils import store_nodejs_output

android_config = PlatformConfig("android", [c.arch_x86, c.arch_arm], DockerBuildSystem)

android_config.cross_config(BuildConfig(
    name="cross-compile-host",
    platform="android",
    host_cwd="$CWD/docker",
    build_cwd="/j2v8",
))

#-----------------------------------------------------------------------
def build_node_js(config, arch):
    store_nodejs_output(config, arch)

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
            make -j4 > /dev/null\"  \
            """,
    ]

android_config.build_step(c.build_node_js, build_node_js)
#-----------------------------------------------------------------------
def build_j2v8_cmake(config, arch):
    return [
        "mkdir -p cmake.out/$PLATFORM.$ARCH",
        "cd cmake.out/$PLATFORM.$ARCH",
        "rm -rf CMakeCache.txt CMakeFiles/",
        """cmake \
            -DCMAKE_BUILD_TYPE=Release \
            -DCMAKE_TOOLCHAIN_FILE=$BUILD_CWD/docker/android/android.$ARCH.toolchain.cmake \
            ../../ \
        """,
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
        "gradle clean assembleRelease"
    ]

android_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
def build_j2v8_junit(config, arch):
    # this assumes that a proper target Android device or emulator is running
    # for the tests to execute on (platform + architecture must match the build settings)
    # TODO: for the cross-compile make sure to start up the appropriate emulator / shutdown after the test runs
    return [
        "gradlew connectedCheck --info"
        #set ANDROID_HOME=C:\PROGRA~2\Android\android-sdk
        #gradlew connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.eclipsesource.v8.V8Test
    ]

android_config.build_step(c.build_j2v8_junit, build_j2v8_junit)
#-----------------------------------------------------------------------
