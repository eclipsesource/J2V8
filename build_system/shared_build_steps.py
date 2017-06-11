import constants as c
import glob
import os
import sys

build_cmd = "mvn verify -DskipTests -e"
clean_build_cmd = "mvn clean verify -DskipTests -e"
run_tests_cmd = "mvn test -e"

def gradleCmd():
    return "gradlew" if os.name == 'nt' else "gradle"

def gradle(cmd):
    return [
        gradleCmd() + " " + cmd,
    ]

def setEnvVar(config, name, value):
    if (config.platform == c.target_win32):
        return ["set \"" + name + "=" + value + "\""]
    else:
        return ["export " + name + "=" + value]

def copyNativeLibs(config, file_arch):
    lib_pattern = "src/main/resources/libj2v8_*"
    libs = glob.glob(lib_pattern)
    rm_libs = [shell("rm", lib)[0] for lib in libs]

    platform_cmake_out = "cmake.out/" + config.platform + "." + config.arch + "/"

    lib_ext = ".so"

    if (config.platform == "win32"):
        platform_cmake_out += "Debug/" if hasattr(config, 'debug') and config.debug else "Release/"
        lib_ext = ".dll"

    platform_lib_path = glob.glob(platform_cmake_out + "*j2v8_*" + file_arch + lib_ext)

    if (len(platform_lib_path) == 0):
        sys.exit("ERROR: Could not find native library for inclusion in platform target package")

    platform_lib_path = platform_lib_path[0]

    lib_target_path = None
    if (config.platform == "Android"):
        lib_target_path = "src/main/jniLibs/" + file_arch + "/libj2v8.so"
    else:
        lib_target_path = "src/main/resources/"

    print "copying native lib from: " + platform_lib_path + " to: " + lib_target_path

    return \
        rm_libs + \
        shell("cp", platform_lib_path + " " + lib_target_path)

def setBuildEnv(config, file_arch):
    return \
        setEnvVar(config, "MVN_PLATFORM_NAME", config.platform) + \
        setEnvVar(config, "MVN_ARCH_NAME", file_arch)

def copyOutput(config, file_arch):
    return \
        shell("mkdir", "build.out") + \
        shell("cp", "target/j2v8_" + config.platform + "_" + file_arch + "-4.7.0-SNAPSHOT.jar build.out/")

def shell(cmd, args):
    return [
        "python $CWD/build_system/" + cmd + ".py " + args,
    ]
