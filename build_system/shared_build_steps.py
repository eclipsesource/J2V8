"""
A collection of commands, constants and functions that are very likely to be
reused between target-platform configurations or build-steps on the same platform.
"""
import glob
import os
import sys
from xml.etree import ElementTree as ET
import xml.parsers.expat

class XmlCommentParser:
    def __init__(self):
        self._tree_builder = ET.TreeBuilder()
        self._parser = xml.parsers.expat.ParserCreate()

        self._parser.CommentHandler = self.handle_comment
        self._parser.StartElementHandler = self._tree_builder.start
        self._parser.EndElementHandler = self._tree_builder.end
        self._parser.CharacterDataHandler = self._tree_builder.data

        self._target = self._tree_builder

    def handle_comment(self, data):
        self._tree_builder.start(ET.Comment, {})
        self._tree_builder.data(data)
        self._tree_builder.end(ET.Comment)

    def feed(self, data):
        self._parser.Parse(data, 0)

    def close(self):
        self._parser.Parse("", 1)
        return self._tree_builder.close()

from . import constants as c
from . import build_settings as s
from . import build_utils as utils

# TODO: add CLI option to override / pass-in custom maven/gradle args
# NOTE: --batch-mode is needed to avoid unicode symbols messing up stdout while unit-testing the build-system
java_build_cmd = "mvn clean verify -e --batch-mode -DskipTests"
java_tests_cmd = "mvn test -e --batch-mode"

# the ./ should work fine on all platforms
# IMPORTANT: on MacOSX the ./ prefix is a strict requirement by some CLI commands !!!
cmake_out_dir = "./cmake.out/$VENDOR-$PLATFORM.$ARCH/"

#-----------------------------------------------------------------------
# Common shell commands & utils
#-----------------------------------------------------------------------
def gradleCmd():
    return "gradlew" if os.name == "nt" else "gradle"

def gradle(cmd):
    return [
        gradleCmd() + " " + cmd,
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
        return ["export " + name + "=\"" + value + "\""]

def setJavaHome(config):
    # NOTE: Docker Linux builds need some special handling, because not all images have
    # a pre-defined JAVA_HOME environment variable
    if (config.platform == c.target_linux and config.cross_agent == "docker"):
        # currently only the Alpine image brings its own java-installation & JAVA_HOME
        # for other Linux images we install the JDK and setup JAVA_HOME manually
        if (config.vendor != c.vendor_alpine):
            print ("Setting JAVA_HOME env-var for Docker Linux build")
            return setEnvVar("JAVA_HOME", "/opt/jdk/jdk1.8.0_131")

    # for any other builds, we can just assume that JAVA_HOME is already set system-wide
    print ("Using system-var JAVA_HOME")
    return []

def setVersionEnv(config):
    return \
        setEnvVar("J2V8_FULL_VERSION", s.J2V8_FULL_VERSION)

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
#-----------------------------------------------------------------------
# Uniform build-steps (cross-platform)
#-----------------------------------------------------------------------
def build_j2v8_jni(config):
    java_class_id = "com.eclipsesource.v8.V8"
    java_class_parts = java_class_id.split(".")
    java_class_filepath = "./target/classes/" + "/".join(java_class_parts) + ".class"

    if (not os.path.exists(java_class_filepath)):
        return [
            "echo WARNING: Could not find " + java_class_parts[-1] + ".class file at path: " + java_class_filepath,
            "echo JNI Header generation will be skipped...",
        ]

    return [
        "echo Generating JNI header files...",
        "cd ./target/classes",
        "javah " + java_class_id,
        ] + cp("com_eclipsesource_v8_V8.h ../../jni/com_eclipsesource_v8_V8Impl.h") + [
        "echo Done",
    ]
#-----------------------------------------------------------------------
# File generators, operations & utils
#-----------------------------------------------------------------------
def copyOutput(config):
    jar_name = outputJarName(config)

    return \
        mkdir("build.out") + \
        cp("target/" + jar_name + " build.out/")

def clearNativeLibs(config):
    """
    Delete previously built native J2V8 libraries from any platforms
    (can be disabled by the "keep_native_libs" config property)
    """
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
    """
    Copy the compiled native J2V8 library (.dll/.dylib/.so) into the Java resources tree
    for inclusion into the later built Java JAR.
    """
    platform_cmake_out = config.inject_env(cmake_out_dir)

    if (utils.is_win32(config.platform)):
        platform_cmake_out += "Debug/" if hasattr(config, 'debug') and config.debug else "Release/"

    lib_pattern = config.inject_env(platform_cmake_out + "*j2v8-*$FILE_ABI.$LIB_EXT")
    platform_lib_path = glob.glob(lib_pattern)

    if (len(platform_lib_path) == 0):
        utils.cli_exit("ERROR: Could not find native library for inclusion in platform target package")

    platform_lib_path = platform_lib_path[0]

    copy_cmds = []

    lib_target_path = None
    if (utils.is_android(config.platform)):
        lib_target_path = config.inject_env("src/main/jniLibs/$FILE_ABI") # directory path
        copy_cmds += mkdir(lib_target_path)
        lib_target_path += "/libj2v8.so" # final lib file path
    else:
        lib_target_path = "src/main/resources/"

    print ("Copying native lib from: " + platform_lib_path + " to: " + lib_target_path)

    copy_cmds += cp(platform_lib_path + " " + lib_target_path)

    return copy_cmds

def apply_maven_null_settings(src_pom_path = "./pom.xml", target_pom_path = None):
    """Copy the Maven pom.xml from src to target, while replacing the necessary XML element values with fixed dummy parameter values"""
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
    """Copy the Maven pom.xml from src to target, while replacing the necessary XML element values based on the given build-step config"""
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
    """
    Copy the Maven pom.xml from src to target, while replacing the XML element values
    based on the values from the hierarchical settings dictionary structure
    """
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

    tree = ET.parse(src_pom_path)
    root = tree.getroot()

    __recurse_maven_settings(settings, __handle_setting)

    tree.write(target_pom_path, default_namespace=pom_ns)
