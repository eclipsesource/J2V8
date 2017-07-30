import constants as c
from build_structures import PlatformConfig
from docker_build import DockerBuildSystem, DockerBuildStep
import shared_build_steps as u
import build_utils as b
import cmake_utils as cmu

android_config = PlatformConfig(c.target_android, [c.arch_x86, c.arch_arm])

android_config.set_cross_configs({
    "docker": DockerBuildStep(
        platform=c.target_android,
        host_cwd="$CWD/docker",
        build_cwd="/j2v8"
    )
})

android_config.set_cross_compilers({
    "docker": DockerBuildSystem
})

android_config.set_file_abis({
    c.arch_arm: "armeabi-v7a",
    c.arch_x86: "x86"
})

#-----------------------------------------------------------------------
def build_node_js(config):
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
            CFLAGS=-fPIC CXXFLAGS=-fPIC make -j4\"  \
            """,
    ]

android_config.build_step(c.build_node_js, build_node_js)
#-----------------------------------------------------------------------
def build_j2v8_cmake(config):
    cmake_vars = cmu.setAllVars(config)
    cmake_toolchain = cmu.setToolchain("$BUILD_CWD/docker/android/android.$ARCH.toolchain.cmake")

    return [
        "mkdir -p " + u.cmake_out_dir,
        "cd " + u.cmake_out_dir,
        "rm -rf CMakeCache.txt CMakeFiles/",
        """cmake \
            -DCMAKE_BUILD_TYPE=Release \
            %(cmake_vars)s \
            %(cmake_toolchain)s \
            ../../ \
        """
        % locals()
    ]

android_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
def build_j2v8_jni(config):
    return [
        "cd " + u.cmake_out_dir,
        "make -j4",
    ]

android_config.build_step(c.build_j2v8_jni, build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_java(config):
    return \
        u.clearNativeLibs(config) + \
        u.copyNativeLibs(config) + \
        u.setVersionEnv(config) + \
        u.gradle("clean assembleRelease")

android_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
def build_j2v8_junit(config):
    # if you are running this step without cross-compiling, it is assumed that a proper target Android device
    # or emulator is running that can execute the tests (platform + architecture must be compatible to the the build settings)

    test_cmds = \
        u.setVersionEnv(config) + \
        u.gradle("spoon")
        # u.gradle("spoon -PtestClass=com.eclipsesource.v8.LibraryLoaderTest,com.eclipsesource.v8.PlatformDetectorTest")
        # u.gradle("connectedCheck --info")

    # we are running a build directly on the host shell
    if (not config.cross_agent):
        # just run the tests on the host directly
        return test_cmds

    # we are cross-compiling, run both the emulator and gradle test-runner in parallel
    else:
        b.apply_file_template(
            "./docker/android/supervisord.template.conf",
            "./docker/android/supervisord.conf",
            lambda x: x.replace("$TEST_CMDS", " && ".join(test_cmds))
        )

        emu_arch = "-arm" if config.arch == c.arch_arm else "64-x86"

        b.apply_file_template(
            "./docker/android/start-emulator.template.sh",
            "./docker/android/start-emulator.sh",
            lambda x: x
                .replace("$IMG_ARCH", config.file_abi)
                .replace("$EMU_ARCH", emu_arch)
        )

        return ["/usr/bin/supervisord -c /j2v8/docker/android/supervisord.conf"]

android_config.build_step(c.build_j2v8_junit, build_j2v8_junit)
#-----------------------------------------------------------------------
