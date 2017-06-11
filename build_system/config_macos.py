import constants as c
from cross_build import BuildStep, PlatformConfig
from vagrant_build import VagrantBuildSystem
import shared_build_steps as u

macos_config = PlatformConfig("macos", [c.arch_x86, c.arch_x64], VagrantBuildSystem)

macos_config.cross_config(BuildStep(
    name="cross-compile-host",
    platform="macos",
    host_cwd="$CWD/vagrant/$PLATFORM",
    build_cwd="/Users/vagrant/j2v8",
))

#-----------------------------------------------------------------------
def build_node_js(config):
    return [
        "cd ./node",
        """./configure              \
            --without-intl          \
            --without-inspector     \
            --dest-cpu=$ARCH        \
            --without-snapshot      \
            --enable-static""",
        # "make clean", # NOTE: make this an on/off option
        "make -j4",
    ]

macos_config.build_step(c.build_node_js, build_node_js)
#-----------------------------------------------------------------------
def build_j2v8_cmake(config):
    return \
        u.shell("mkdir", "cmake.out/$PLATFORM.$ARCH") + \
        ["cd cmake.out/$PLATFORM.$ARCH"] + \
        u.shell("rm", "CMakeCache.txt CMakeFiles/") + \
        ["cmake ../../"]

macos_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
def build_j2v8_jni(config):
    return [
        "cd cmake.out/$PLATFORM.$ARCH",
        "make -j4",
    ]

macos_config.build_step(c.build_j2v8_jni, build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_java(config):
    file_arch = "x86_64" if config.arch == c.arch_x64 else "x86"
    return \
        u.copyNativeLibs(config, file_arch) + \
        u.setBuildEnv(config, file_arch) + \
        [u.build_cmd] + \
        u.copyOutput(config, file_arch)

macos_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
def build_j2v8_junit(config):
    file_arch = "x86_64" if config.arch == c.arch_x64 else "x86"
    return \
        u.setBuildEnv(config, file_arch) + \
        [u.run_tests_cmd]

macos_config.build_step(c.build_j2v8_junit, build_j2v8_junit)
#-----------------------------------------------------------------------
