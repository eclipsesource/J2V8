import constants as c
from build_structures import PlatformConfig
from docker_build import DockerBuildSystem, DockerBuildStep
import shared_build_steps as u
import cmake_utils as cmu

linux_config = PlatformConfig(c.target_linux, [c.arch_x86, c.arch_x64])

linux_config.set_cross_configs({
    "docker": DockerBuildStep(
        platform=c.target_linux,
        host_cwd="$CWD/docker",
        build_cwd="/j2v8"
    )
})

linux_config.set_cross_compilers({
    "docker": DockerBuildSystem
})

linux_config.set_file_abis({
    c.arch_x64: "x86_64",
    c.arch_x86: "x86_32",
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
        # "make clean", # TODO: make this an on/off option
        "CFLAGS=-fPIC CXXFLAGS=-fPIC make -j4",
    ]

linux_config.build_step(c.build_node_js, build_node_js)
#-----------------------------------------------------------------------
def build_j2v8_cmake(config):
    cmake_vars = cmu.setAllVars(config)

    # NOTE: uses Python string interpolation (see: https://stackoverflow.com/a/4450610)
    return \
        u.mkdir(u.cmake_out_dir) + \
        ["cd " + u.cmake_out_dir] + \
        u.rm("CMakeCache.txt CMakeFiles/") + \
        u.setJavaHome(config) + \
        ["""cmake \
            -DCMAKE_BUILD_TYPE=Release \
            %(cmake_vars)s \
            ../../ \
        """
        % locals()]

linux_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
def build_j2v8_jni(config):
    return [
        "cd " + u.cmake_out_dir,
        "make -j4",
    ]

linux_config.build_step(c.build_j2v8_jni, build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_optimize(config):
    # NOTE: execstack / strip are not part of the alpine tools, therefore we just skip this step
    if config.vendor == c.vendor_alpine:
        return ["echo Skipped..."]

    lib_path = u.outputLibPath(config)
    return [
        "execstack -c " + lib_path,
        "strip --strip-unneeded -R .note -R .comment " + lib_path,
    ]

linux_config.build_step(c.build_j2v8_optimize, build_j2v8_optimize)
#-----------------------------------------------------------------------
def build_j2v8_java(config):
    u.apply_maven_config_settings(config)

    return \
        u.clearNativeLibs(config) + \
        u.copyNativeLibs(config) + \
        u.setJavaHome(config) + \
        [u.build_cmd] + \
        u.copyOutput(config)

linux_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
def build_j2v8_junit(config):
    return \
        [u.run_tests_cmd]

linux_config.build_step(c.build_j2v8_junit, build_j2v8_junit)
#-----------------------------------------------------------------------
