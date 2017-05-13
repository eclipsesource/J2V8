import constants as c
from cross_build import BuildConfig, PlatformConfig
from vagrant_build import VagrantBuildSystem

macos_config = PlatformConfig("macos", [c.arch_x86, c.arch_x64], VagrantBuildSystem)

macos_config.cross_config(BuildConfig(
    name="cross-compile-host",
    platform="macos",
    host_cwd="$CWD/vagrant/$PLATFORM",
    build_cwd="/Users/vagrant/j2v8",
))

#-----------------------------------------------------------------------
def build_node_js(config, arch):
    return [
        "cd ./node",
        """./configure              \
            --without-intl          \
            --without-inspector     \
            --dest-cpu=$ARCH        \
            --without-snapshot      \
            --enable-static""",
        "make -j4",
    ]
macos_config.build_step(c.build_node_js, build_node_js)
#-----------------------------------------------------------------------
def build_j2v8_jni(config, arch):
    return [
        "mkdir -p cmake.out/$PLATFORM.$ARCH",
        "cd cmake.out/$PLATFORM.$ARCH",
        "rm -rf CMakeCache.txt",
        "cmake ../../",
        "make -j4",
    ]

macos_config.build_step(c.build_j2v8_jni, build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_java(config, arch):
    return [
        "export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home",
        "export PATH=/opt/apache-maven-3.5.0/bin:$PATH",
        "export MVN_PLATFORM_NAME=macosx",
        "export MVN_ARCH_NAME=" + ("x86_x64" if arch == c.arch_x64 else "x86"),
        "mvn clean verify -e",
    ]
macos_config.build_step(c.build_j2v8_java, build_j2v8_java)
#-----------------------------------------------------------------------
