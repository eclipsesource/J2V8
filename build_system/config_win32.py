import os
from . import constants as c
from .build_structures import PlatformConfig
from .docker_build import DockerBuildSystem, DockerBuildStep
from .vagrant_build import VagrantBuildSystem, VagrantBuildStep
from . import java_build_steps as j
from . import shared_build_steps as u
from . import cmake_utils as cmu

win32_config = PlatformConfig(c.target_win32, [c.arch_x86, c.arch_x64])

win32_config.set_cross_configs({
    "docker": DockerBuildStep(
        platform=c.target_win32,
        host_cwd="$CWD/docker",
        build_cwd="C:/j2v8"
    ),
    "vagrant": VagrantBuildStep(
        platform=c.target_win32,
        host_cwd="$CWD/vagrant/$PLATFORM",
        build_cwd="C:/j2v8"
    )
})

win32_config.set_cross_compilers({
    "docker": DockerBuildSystem,
    "vagrant": VagrantBuildSystem,
})

win32_config.set_file_abis({
    c.arch_x64: "x86_64",
    c.arch_x86: "x86_32",
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
    cmake_vars = cmu.setAllVars(config)
    cmake_gen_suffix = " Win64" if config.arch == c.arch_x64 else ""
    cmake_pdb_fix_flag = cmu.setWin32PdbDockerFix(config)

    # NOTE: uses Python string interpolation (see: https://stackoverflow.com/a/4450610)
    return \
        u.mkdir(u.cmake_out_dir) + \
        ["cd " + u.cmake_out_dir] + \
        u.rm("CMakeCache.txt CMakeFiles/") + \
        ["""cmake \
            ../../ \
            %(cmake_vars)s \
            %(cmake_pdb_fix_flag)s \
            -G"Visual Studio 14 2015%(cmake_gen_suffix)s"
        """
        % locals()]

win32_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
win32_config.build_step(c.build_j2v8_jni, u.build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_cpp(config):
    # show docker container memory usage / limit
    show_mem = ["powershell C:/j2v8/docker/win32/mem.ps1"] if config.cross_agent == "docker" else []

    return \
        show_mem + \
        [
            "cd " + u.cmake_out_dir,
            "msbuild j2v8.sln /property:Configuration=Release",
        ] + \
        show_mem

win32_config.build_step(c.build_j2v8_cpp, build_j2v8_cpp)
#-----------------------------------------------------------------------
j.add_java_build_step(win32_config)
#-----------------------------------------------------------------------
j.add_java_test_step(win32_config)
#-----------------------------------------------------------------------
