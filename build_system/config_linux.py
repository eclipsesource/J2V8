import constants as c
from cross_build import BuildStep, PlatformConfig
from docker_build import DockerBuildSystem
import shared_build_steps as u

linux_config = PlatformConfig(c.target_linux, [c.arch_x86, c.arch_x64])

linux_config.set_cross_configs({
    "docker": BuildStep(
        name="cross-compile-host",
        platform=c.target_linux,
        host_cwd="$CWD/docker",
        build_cwd="/j2v8",
    )
})

linux_config.set_cross_compilers({
    "docker": DockerBuildSystem
})

linux_config.set_file_abis({
    c.arch_x64: "x86_64",
    c.arch_x86: "x86"
})

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
def build_j2v8_optimize(config):
    file_abi = config.target.file_abi(config.arch)
    return [
        "execstack -c cmake.out/$PLATFORM.$ARCH/libj2v8_linux_" + file_abi + ".so",
        "strip --strip-unneeded -R .note -R .comment cmake.out/$PLATFORM.$ARCH/libj2v8_linux_" + file_abi + ".so",
    ]

linux_config.build_step(c.build_j2v8_optimize, build_j2v8_optimize)
#-----------------------------------------------------------------------
def build_j2v8_java(config):
    return \
        u.clearNativeLibs(config) + \
        u.copyNativeLibs(config) + \
        u.setBuildEnv(config) + \
        [u.build_cmd] + \
        u.copyOutput(config)

linux_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
def build_j2v8_junit(config):
    return \
        u.setBuildEnv(config) + \
        [u.run_tests_cmd]

linux_config.build_step(c.build_j2v8_junit, build_j2v8_junit)
#-----------------------------------------------------------------------
