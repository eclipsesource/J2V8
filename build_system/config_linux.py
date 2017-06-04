import constants as c
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem

linux_config = PlatformConfig("linux", [c.arch_x86, c.arch_x64], DockerBuildSystem)

linux_config.cross_config(BuildConfig(
    name="cross-compile-host",
    platform="linux",
    # host_cwd="$CWD/docker/$PLATFORM",
    host_cwd="$CWD/docker",
    build_cwd="/j2v8",
))

#-----------------------------------------------------------------------
def build_node_js(config, arch):
    # TODO: create "BuildContext" class and pass BuildSystem arround to use utils like inject_env

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
def build_j2v8_cmake(config, arch):
    return [
        "mkdir -p cmake.out/$PLATFORM.$ARCH",
        "cd cmake.out/$PLATFORM.$ARCH",
        "rm -rf CMakeCache.txt CMakeFiles/",
        "cmake ../../",
    ]

linux_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
def build_j2v8_jni(config, arch):
    return [
        "cd cmake.out/$PLATFORM.$ARCH",
        "make -j4",
    ]

linux_config.build_step(c.build_j2v8_jni, build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_java(config, arch):
    file_arch = "x86_64" if arch == c.arch_x64 else "x86"
    return [
        "export MVN_PLATFORM_NAME=linux",
        "export MVN_ARCH_NAME=" + file_arch,
        "mvn verify -DskipTests -e",
        # TODO: maybe make j2v8tests a separate build-step
        # "mvn -DskipTests=true verify",
        "mkdir -p build.out",
        "cp target/j2v8_linux_" + file_arch + "-4.7.0-SNAPSHOT.jar build.out/",
    ]

linux_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
def build_j2v8_junit(config, arch):
    file_arch = "x86_64" if arch == c.arch_x64 else "x86"
    return [
        "export MVN_PLATFORM_NAME=linux",
        "export MVN_ARCH_NAME=" + file_arch,
        "mvn test -e"
    ]

linux_config.build_step(c.build_j2v8_junit, build_j2v8_junit)
#-----------------------------------------------------------------------
