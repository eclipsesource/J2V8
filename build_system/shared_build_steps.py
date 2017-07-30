import glob
import os
import sys
import xml.etree.ElementTree as ET

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

def outputJarName(config):
    return config.inject_env("j2v8_$VENDOR-$PLATFORM_$FILE_ABI-$J2V8_FULL_VERSION.jar")

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

def setVersionEnv(config):
    return \
        setEnvVar("J2V8_FULL_VERSION", s.J2V8_FULL_VERSION)

def copyOutput(config):
    jar_name = outputJarName(config)

    return \
        mkdir("build.out") + \
        cp("target/" + jar_name + " build.out/")

def shell(cmd, args):
    """
    Invokes the cross-platform polyfill for the shell command defined by the 'cmd' parameter
    """
    return ["python $CWD/build_system/polyfills/" + cmd + ".py " + args]

def cp(args):
    """Invokes the cross-platform polyfill for the 'cp' shell command"""
    return shell("cp", args)

def mkdir(args):
    """Invokes the cross-platform polyfill for the 'mkdir' shell command"""
    return shell("mkdir", args)

def rm(args):
    """Invokes the cross-platform polyfill for the 'rm' shell command"""
    return shell("rm", args)

def clearNativeLibs(config):
    # the CLI can override this step
    if (config.keep_native_libs):
        print("Native libraries not cleared...")
        return []

    def clearLibs(lib_pattern):
        libs = glob.glob(lib_pattern)
        return [rm(lib)[0] for lib in libs]

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
        copy_cmds += mkdir(lib_target_path)
        lib_target_path += "/libj2v8.so" # final lib file path
    else:
        lib_target_path = "src/main/resources/"

    print "copying native lib from: " + platform_lib_path + " to: " + lib_target_path

    copy_cmds += cp(platform_lib_path + " " + lib_target_path)

    return copy_cmds

def apply_maven_null_settings(src_pom_path = "./pom.xml", target_pom_path = None):
    maven_settings = {
        "properties": {
            "os": "undefined",
            "arch": "undefined",
        },
        "artifactId": "undefined",
        "version": "undefined",
        "name": "undefined",
    }

    apply_maven_settings(maven_settings, src_pom_path, target_pom_path)

def apply_maven_config_settings(config, src_pom_path = "./pom.xml", target_pom_path = None):
    os = config.inject_env("$VENDOR-$PLATFORM")
    arch = config.file_abi
    version = s.J2V8_FULL_VERSION
    name = config.inject_env("j2v8_$VENDOR-$PLATFORM_$FILE_ABI")

    maven_settings = {
        "properties": {
            "os": os,
            "arch": arch,
        },
        "artifactId": name,
        "version": version,
        "name": name,
    }

    apply_maven_settings(maven_settings, src_pom_path, target_pom_path)

def apply_maven_settings(settings, src_pom_path = "./pom.xml", target_pom_path = None):
    #-----------------------------------------------------------------------
    pom_ns = "http://maven.apache.org/POM/4.0.0"
    ns = {"pom": pom_ns}
    #-----------------------------------------------------------------------
    def __recurse_maven_settings(settings, callback, curr_path = None):
        if (curr_path is None):
            curr_path = []

        for key in settings:
            value = settings.get(key)

            curr_path.append(key)

            if isinstance(value, dict):
                __recurse_maven_settings(value, callback, curr_path)
            else:
                callback(curr_path, value)

            curr_path.pop()
    #-----------------------------------------------------------------------
    def __handle_setting(path, value):
        xpath = "." + "/pom:".join([""] + path)
        node = root.find(xpath, ns)
        node.text = value
        return
    #-----------------------------------------------------------------------

    target_pom_path = target_pom_path or src_pom_path

    print "Updating Maven configuration (" + target_pom_path + ")..."

    tree = ET.parse(src_pom_path)
    root = tree.getroot()

    __recurse_maven_settings(settings, __handle_setting)

    tree.write(target_pom_path, default_namespace=pom_ns)
