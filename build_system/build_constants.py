
import constants as c

from config_android import android_config
from config_linux import linux_config
from config_macos import macos_config
from config_win32 import win32_config

build_step_sequence = [
    c.build_node_js,
    c.build_j2v8_cmake,
    c.build_j2v8_jni,
    c.build_j2v8_optimize,
    c.build_j2v8_java,
    c.build_j2v8_junit,
]

composite_steps = [
    # composites
    c.build_all,
    c.build_native,
    c.build_j2v8,
    # aliases
    c.build_java,
    c.build_test,
]

platform_targets = {
    c.target_android: android_config,
    c.target_linux: linux_config,
    c.target_macos: macos_config,
    c.target_win32: win32_config,
}

avail_targets = platform_targets.keys()

avail_architectures = [
    c.arch_x86,
    c.arch_x64,
    c.arch_arm,
]

avail_build_steps = build_step_sequence + composite_steps
