import constants as c
from cross_build import BuildStep, PlatformConfig
from docker_build import DockerBuildSystem
import shared_build_steps as u

linux_config = PlatformConfig("linux", [c.arch_x86, c.arch_x64], DockerBuildSystem)

linux_config.cross_config(BuildStep(
    name="cross-compile-host",
    platform="linux",
    host_cwd="$CWD/docker",
    build_cwd="/j2v8",
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
        "CFLAGS=-fPIC CXXFLAGS=-fPIC make -j4",
    ]

linux_config.build_step(c.build_node_js, build_node_js)
#-----------------------------------------------------------------------
def build_j2v8_cmake(config):
    return \
        u.shell("mkdir", "cmake.out/$PLATFORM.$ARCH") + \
        ["cd cmake.out/$PLATFORM.$ARCH"] + \
        u.shell("rm", "CMakeCache.txt CMakeFiles/") + \
        ["cmake ../../"]

linux_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
def build_j2v8_jni(config):
    return [
        "cd cmake.out/$PLATFORM.$ARCH",
        "make -j4",
    ]

linux_config.build_step(c.build_j2v8_jni, build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_java(config):
    file_arch = "x86_64" if config.arch == c.arch_x64 else "x86"
    return \
        u.copyNativeLibs(config, file_arch) + \
        u.setBuildEnv(config, file_arch) + \
        [u.build_cmd] + \
        u.copyOutput(config, file_arch)

linux_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
def build_j2v8_junit(config):
    file_arch = "x86_64" if config.arch == c.arch_x64 else "x86"
    return \
        u.setBuildEnv(config, file_arch) + \
        [u.run_tests_cmd]

linux_config.build_step(c.build_j2v8_junit, build_j2v8_junit)
#-----------------------------------------------------------------------
