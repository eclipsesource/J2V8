import glob
import os
import sys

import constants as c
import build_settings as s
import build_utils as utils

build_cmd = "mvn verify -DskipTests -e"
clean_build_cmd = "mvn clean verify -DskipTests -e"
run_tests_cmd = "mvn test -e"

def gradleCmd():
    return "gradlew" if os.name == "nt" else "gradle"

def gradle(cmd):
    return [
        gradleCmd() + " " + cmd,
    ]

def setEnvVar(name, value):
    if (os.name == "nt"):
        return ["set \"" + name + "=" + value + "\""]
    else:
        return ["export " + name + "=" + value]

def clearNativeLibs(config):
    lib_pattern = "src/main/resources/libj2v8_*"

    if (utils.is_android(config.platform)):
        lib_pattern = "src/main/jniLibs/*/libj2v8.so"

    libs = glob.glob(lib_pattern)
    rm_libs = [shell("rm", lib)[0] for lib in libs]

    return rm_libs

def copyNativeLibs(config):
    file_abi = config.target.file_abi(config.arch)

    platform_cmake_out = "cmake.out/" + config.platform + "." + config.arch + "/"
    lib_ext = ".so"

    if (utils.is_win32(config.platform)):
        platform_cmake_out += "Debug/" if hasattr(config, 'debug') and config.debug else "Release/"
        lib_ext = ".dll"

    elif (utils.is_macos(config.platform)):
        lib_ext = ".dylib"

    lib_pattern = platform_cmake_out + "*j2v8_*" + file_abi + lib_ext
    platform_lib_path = glob.glob(lib_pattern)

    if (len(platform_lib_path) == 0):
        sys.exit("ERROR: Could not find native library for inclusion in platform target package")

    platform_lib_path = platform_lib_path[0]

    copy_cmds = []

    lib_target_path = None
    if (utils.is_android(config.platform)):
        lib_target_path = "src/main/jniLibs/" + file_abi # directory path
        copy_cmds += shell("mkdir", lib_target_path)
        lib_target_path += "/libj2v8.so" # final lib file path
    else:
        lib_target_path = "src/main/resources/"

    print "copying native lib from: " + platform_lib_path + " to: " + lib_target_path

    copy_cmds += shell("cp", platform_lib_path + " " + lib_target_path)

    return copy_cmds

def setBuildEnv(config):
    file_abi = config.target.file_abi(config.arch)

    return \
        setEnvVar("J2V8_PLATFORM_NAME", config.platform) + \
        setEnvVar("J2V8_ARCH_NAME", file_abi) + \
        setEnvVar("J2V8_FULL_VERSION", s.J2V8_FULL_VERSION)

def setVersionEnv(config):
    return \
        setEnvVar("J2V8_FULL_VERSION", s.J2V8_FULL_VERSION)

def copyOutput(config):
    file_abi = config.target.file_abi(config.arch)

    return \
        shell("mkdir", "build.out") + \
        shell("cp", "target/j2v8_" + config.platform + "_" + file_abi + "-" + s.J2V8_FULL_VERSION + ".jar build.out/")

def shell(cmd, args):
    return [
        "python $CWD/build_system/" + cmd + ".py " + args,
    ]
