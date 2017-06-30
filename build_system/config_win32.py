import os
import constants as c
from cross_build import BuildStep, PlatformConfig
from docker_build import DockerBuildSystem
from vagrant_build import VagrantBuildSystem
import shared_build_steps as u

win32_config = PlatformConfig(c.target_win32, [c.arch_x86, c.arch_x64])

win32_config.set_cross_configs({
    "docker": BuildStep(
        name="docker-compile-host",
        platform=c.target_win32,
        host_cwd="$CWD/docker",
        build_cwd="C:/j2v8",
    ),
    "vagrant": BuildStep(
        name="vagrant-compile-host",
        platform=c.target_win32,
        host_cwd="$CWD/vagrant/$PLATFORM",
        build_cwd="C:/j2v8",
    )
})

win32_config.set_cross_compilers({
    "docker": DockerBuildSystem,
    "vagrant": VagrantBuildSystem,
})

win32_config.set_file_abis({
    c.arch_x64: "x86_64",
    c.arch_x86: "x86"
})

#-----------------------------------------------------------------------
def build_node_js(config):
    return [
        "cd ./node",
        "vcbuild.bat release $ARCH",
    ]

win32_config.build_step(c.build_node_js, build_node_js)
#-----------------------------------------------------------------------
def build_j2v8_cmake(config):
    cmake_gen_suffix = " Win64" if config.arch == c.arch_x64 else ""
    cmake_x_compile_flag = "-DJ2V8_CROSS_COMPILE=1" if config.cross_agent else ""
    cmake_pdb_fix_flag = "-DJ2V8_WIN32_PDB_DOCKER_FIX=1" if config.cross_agent == "docker" else ""
    return \
        u.shell("mkdir", "cmake.out/$PLATFORM.$ARCH") + \
        ["cd cmake.out\\$PLATFORM.$ARCH"] + \
        u.shell("rm", "CMakeCache.txt CMakeFiles/") + \
        ["cmake ..\\..\\ " + cmake_x_compile_flag + " " + cmake_pdb_fix_flag + " -G\"Visual Studio 14 2015" + cmake_gen_suffix + "\""]

win32_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
def build_j2v8_jni(config):
    # show docker container memory usage / limit
    show_mem = ["powershell C:/temp/mem.ps1"] if config.cross_agent == "docker" else []

    return \
        show_mem + \
        [
            "cd cmake.out\$PLATFORM.$ARCH",
            "msbuild j2v8.sln /property:Configuration=Release",
        ] + \
        show_mem

win32_config.build_step(c.build_j2v8_jni, build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_java(config):
    return \
        u.clearNativeLibs(config) + \
        u.copyNativeLibs(config) + \
        u.setBuildEnv(config) + \
        [u.build_cmd] + \
        u.copyOutput(config)

win32_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
def build_j2v8_junit(config):
    return \
        u.setBuildEnv(config) + \
        [u.run_tests_cmd]

win32_config.build_step(c.build_j2v8_junit, build_j2v8_junit)
#-----------------------------------------------------------------------
