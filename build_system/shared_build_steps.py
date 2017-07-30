import glob
import os
import sys

import constants as c
import build_settings as s
import build_utils as utils

# TODO: add CLI option to override / pass-in custom maven/gradle args
build_cmd = "mvn clean verify -DskipTests -e"
run_tests_cmd = "mvn test -e"# -Dtest=V8RuntimeNotLoadedTest"

# the ./ should work fine on all platforms
# IMPORTANT: on MacOSX the ./ prefix is a strict requirement by some CLI commands !!!
cmake_out_dir = "./cmake.out/$VENDOR-$PLATFORM.$ARCH/"

def gradleCmd():
    return "gradlew" if os.name == "nt" else "gradle"

def gradle(cmd):
    return [
        gradleCmd() + " --daemon " + cmd,
    ]

def outputLibName(config):
    return config.inject_env("libj2v8-$VENDOR-$PLATFORM-$FILE_ABI.$LIB_EXT")

def outputLibPath(config):
    return cmake_out_dir + "/" + outputLibName(config)

def setEnvVar(name, value):
    if (os.name == "nt"):
        return ["set \"" + name + "=" + value + "\""]
    else:
        return ["export " + name + "=" + value]

def setJavaHome(config):
    # NOTE: when running docker alpine-linux builds, we don't want to overwrite JAVA_HOME
    if (config.vendor == c.vendor_alpine and config.cross_agent == "docker"):
        return []

    return setEnvVar("JAVA_HOME", "/opt/jdk/jdk1.8.0_131")

def clearNativeLibs(config):
    # the CLI can override this step
    if (config.keep_native_libs):
        print("Native libraries not cleared...")
        return []

    def clearLibs(lib_pattern):
        libs = glob.glob(lib_pattern)
        return [shell("rm", lib)[0] for lib in libs]

    rm_libs = \
        clearLibs("src/main/resources/libj2v8*") + \
        clearLibs("src/main/jniLibs/*/libj2v8.so")

    return rm_libs

def copyNativeLibs(config):
    platform_cmake_out = config.inject_env(cmake_out_dir)

    if (utils.is_win32(config.platform)):
        platform_cmake_out += "Debug/" if hasattr(config, 'debug') and config.debug else "Release/"

    lib_pattern = config.inject_env(platform_cmake_out + "*j2v8-*$FILE_ABI.$LIB_EXT")
    platform_lib_path = glob.glob(lib_pattern)

    if (len(platform_lib_path) == 0):
        sys.exit("ERROR: Could not find native library for inclusion in platform target package")

    platform_lib_path = platform_lib_path[0]

    copy_cmds = []

    lib_target_path = None
    if (utils.is_android(config.platform)):
        lib_target_path = config.inject_env("src/main/jniLibs/$FILE_ABI") # directory path
        copy_cmds += shell("mkdir", lib_target_path)
        lib_target_path += "/libj2v8.so" # final lib file path
    else:
        lib_target_path = "src/main/resources/"

    print "copying native lib from: " + platform_lib_path + " to: " + lib_target_path

    copy_cmds += shell("cp", platform_lib_path + " " + lib_target_path)

    return copy_cmds

def setBuildEnv(config):
    return \
        setEnvVar("J2V8_PLATFORM_NAME", config.platform) + \
        setEnvVar("J2V8_ARCH_NAME", config.file_abi) + \
        setEnvVar("J2V8_FULL_VERSION", s.J2V8_FULL_VERSION)

def setVersionEnv(config):
    return \
        setEnvVar("J2V8_FULL_VERSION", s.J2V8_FULL_VERSION)

def copyOutput(config):
    return \
        shell("mkdir", "build.out") + \
        shell("cp", "target/j2v8_$PLATFORM_$FILE_ABI-$J2V8_FULL_VERSION.jar build.out/")

def shell(cmd, args):
    return [
        "python $CWD/build_system/" + cmd + ".py " + args,
    ]
