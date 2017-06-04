import constants as c
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem

android_config = PlatformConfig("android", [c.arch_x86, c.arch_arm], DockerBuildSystem)

android_config.cross_config(BuildConfig(
    name="cross-compile-host",
    platform="android",
    host_cwd="$CWD/docker",
    build_cwd="/j2v8",
))

#-----------------------------------------------------------------------
def build_node_js(config, arch):
    # TODO: apply c++11 & suppress warnings
    # TODO: redirect stdout of g++ (currently not piped correctly)
    # CCFLAGS='-std=c++11 -Wno-ignored-qualifiers -Wno-sign-compare' CXXFLAGS='-std=c++11 -Wno-ignored-qualifiers -Wno-sign-compare' make -j4 > /dev/null'  \

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

    # emu_start = [
    #     "echo no | $ANDROID_HOME/tools/bin/avdmanager create avd -n test -k 'system-images;android-19;default;$ARCH'",
    #     "$ANDROID_HOME/emulator/emulator64-x86 -avd test -noaudio -no-window -memory 1024 -gpu on -verbose -qemu -usbdevice tablet -vnc :0",
    # ]
    # TODO: gradlew on windows
    run_tests = [
        "gradle connectedCheck --info"
    ]

    # TODO: refactor to be able to check CLI / build params
    params_cross_enabled = True
    if (params_cross_enabled):
        supervisord_conf = None
        with open("./docker/android/supervisord.template.conf", 'r') as f:
            supervisord_conf = f.read()

        # TODO: generic inject env utility ???
        supervisord_conf = supervisord_conf.replace("$TEST_CMDS", " && ".join(run_tests))

        with open("./docker/android/supervisord.conf", 'w') as f:
            f.write(supervisord_conf)

        emulator_start = None
        with open("./docker/android/start-emulator.template.sh", 'r') as f:
            emulator_start = f.read()

        # TODO: generic inject env utility ???
        image_arch = "armeabi-v7a" if arch == c.arch_arm else arch
        emu_arch = "-arm" if arch == c.arch_arm else "64-x86"
        emulator_start = emulator_start.replace("$IMG_ARCH", image_arch).replace("$EMU_ARCH", emu_arch)

        with open("./docker/android/start-emulator.sh", 'w') as f:
            f.write(emulator_start)

        return [
            "/usr/bin/supervisord -c /j2v8/docker/android/supervisord.conf"
        ]
    else:
        # just run the tests
        return run_tests

    #if -x
    # create supervisord conf
    # p1 = android emu
    # p2 = wait via adb for emu, then run tests
    # run supervisord
    #else
    # just run tests

    return [
        # "$ANDROID_HOME/tools/android create avd -f -n test -t android-19 --abi default/$ARCH",

        # V1
        # TODO: how to make this cross platform / cross compile aware
        # "echo no | $ANDROID_HOME/tools/bin/avdmanager create avd -n test -k 'system-images;android-19;default;$ARCH'",
        # "$ANDROID_HOME/emulator/emulator64-x86 -avd test -noaudio -no-window -memory 1024 -gpu on -verbose -qemu -usbdevice tablet -vnc :0",

        # V2
        # "echo no | /usr/local/android-sdk/tools/android create avd -f -n test -t android-19 --abi default/$ARCH",
        # "echo no | /usr/local/android-sdk/tools/emulator64-$ARCH -avd test -noaudio -no-window -gpu off -verbose -qemu -usbdevice tablet -vnc :0",


        # setup complete, now run the actual tests
        "gradlew connectedCheck --info"
        #set ANDROID_HOME=C:\PROGRA~2\Android\android-sdk
        #gradlew connectedCheck -Pandroid.testInstrumentationRunnerArguments.class=com.eclipsesource.v8.V8Test
    ]

android_config.build_step(c.build_j2v8_junit, build_j2v8_junit)
#-----------------------------------------------------------------------
