"""
Contains the essential lists/map structures that are referenced by the build process & CLI
"""
import collections

import constants as c

from config_android import android_config
from config_linux import linux_config
from config_macos import macos_config
from config_win32 import win32_config

CLIStep = collections.namedtuple("CLIStep", "id help")

#-----------------------------------------------------------------------
# Build-steps lists, maps and sequences
#-----------------------------------------------------------------------
atomic_build_steps = [
    CLIStep(c.build_node_js, "          Builds the Node.js & V8 dependency artifacts that are later linked into the J2V8 native bridge code.\n" + 
                       "                (only works if the Node.js source was checked out into the J2V8 ./node directory)"),
    CLIStep(c.build_j2v8_cmake, "       Uses CMake to generate the native Makefiles / IDE project files to later build the J2V8 C++ native bridge shared libraries."),
    CLIStep(c.build_j2v8_jni, "         Generate the J2V8 JNI C++ Header files."),
    CLIStep(c.build_j2v8_cpp, "         Compile and link the J2V8 C++ shared libraries (.so/.dylib/.dll), which provide the JNI bridge to interop with the C++ code of Node.js / V8."),
    CLIStep(c.build_j2v8_optimize, "    The native J2V8 libraries are optimized for performance and/or filesize by using the available tools of the target-platform / compiler-toolchain."),
    CLIStep(c.build_j2v8_java, "        Compiles the Java source code and packages it, including the previously built native libraries, into the final package artifacts.\n" +
                       "                For the execution of this build-step Maven (Java) or Gradle (Android) are used for the respective target platforms."),
    CLIStep(c.build_j2v8_test, "       Runs the Java (JUnit/Gradle) unit tests."),
]

# build_steps_help = dict(atomic_build_steps)

atomic_build_step_sequence = [s.id for s in atomic_build_steps]

advanced_steps = [
    # atomic aliases
    CLIStep(c.build_java, "            Alias for " + c.build_j2v8_java),
    CLIStep(c.build_test, "            Alias for " + c.build_j2v8_test),

    # multi-step aliases
    CLIStep(c.build_all, "             Run all build steps."),
    CLIStep(c.build_native, "          Build only the native parts. (includes nodejs)"),
    CLIStep(c.build_j2v8, "            Run all build steps to build J2V8 (this does not try to build Node.js)\n" +
                      "                This is useful when building with a pre-compiled Node.js dependency package."),
]

advanced_steps_list = [s.id for s in advanced_steps]

avail_build_steps = atomic_build_step_sequence + advanced_steps_list

#-----------------------------------------------------------------------
# Build execution core function
#-----------------------------------------------------------------------
platform_configs = {
    c.target_android: android_config,
    c.target_linux: linux_config,
    c.target_macos: macos_config,
    c.target_win32: win32_config,
}

avail_targets = platform_configs.keys()

avail_architectures = [
    c.arch_x86,
    c.arch_x86_64,
    c.arch_x64,
    c.arch_arm,
    c.arch_arm64
]
