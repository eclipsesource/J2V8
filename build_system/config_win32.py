import constants as c
from cross_build import BuildStep, PlatformConfig
from docker_build import DockerBuildSystem
import shared_build_steps as u

win32_config = PlatformConfig("win32", [c.arch_x86, c.arch_x64], DockerBuildSystem)

win32_config.cross_config(BuildStep(
    name="cross-compile-host",
    platform="win32",
    host_cwd="$CWD/docker",
    build_cwd="C:/j2v8",
))

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
    cmake_x_compile_flag = "-DJ2V8_CROSS_COMPILE=1" if config.build_agent else ""
    cmake_pdb_fix_flag = "-DJ2V8_WIN32_PDB_DOCKER_FIX=1" if config.build_agent else ""
    return \
        u.shell("mkdir", "cmake.out/$PLATFORM.$ARCH") + \
        ["cd cmake.out\\$PLATFORM.$ARCH"] + \
        u.shell("rm", "CMakeCache.txt CMakeFiles/") + \
        ["cmake ..\\..\\ " + cmake_x_compile_flag + " " + cmake_pdb_fix_flag + " -G\"Visual Studio 14 2015" + cmake_gen_suffix + "\""]

win32_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
def build_j2v8_jni(config):
    # show docker container memory usage / limit
    show_mem = ["powershell C:/temp/mem.ps1"] if config.build_agent else []

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
    file_arch = "x86_64" if config.arch == c.arch_x64 else "x86"
    return \
        u.copyNativeLibs(config, file_arch) + \
        u.setBuildEnv(config, file_arch) + \
        [u.build_cmd] + \
        u.copyOutput(config, file_arch)

win32_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
def build_j2v8_junit(config):
    file_arch = "x86_64" if config.arch == c.arch_x64 else "x86"
    return \
        u.setBuildEnv(config, file_arch) + \
        [u.run_tests_cmd]

win32_config.build_step(c.build_j2v8_junit, build_j2v8_junit)
#-----------------------------------------------------------------------
