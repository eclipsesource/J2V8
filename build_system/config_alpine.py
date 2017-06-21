import constants as c
from cross_build import BuildStep, PlatformConfig
from docker_build import DockerBuildSystem
import config_linux as lc

alpine_config = PlatformConfig(c.target_alpine, [c.arch_x86, c.arch_x64], DockerBuildSystem)

alpine_config.cross_config(BuildStep(
    name="cross-compile-host",
    platform=c.target_alpine,
    host_cwd="$CWD/docker",
    build_cwd="/j2v8",
))

alpine_config.set_file_abis({
    c.arch_x64: "x86_64",
    c.arch_x86: "x86"
})

# Alpine build steps are the same as for linux  - just the build environment is different
alpine_config.build_step(c.build_node_js, lc.build_node_js)
alpine_config.build_step(c.build_j2v8_cmake, lc.build_j2v8_cmake)
alpine_config.build_step(c.build_j2v8_jni, lc.build_j2v8_jni)
alpine_config.build_step(c.build_j2v8_java, lc.build_j2v8_java)
alpine_config.build_step(c.build_j2v8_junit, lc.build_j2v8_junit)