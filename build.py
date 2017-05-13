import argparse
import sys
import build_system.constants as c
from build_system.shell_build import ShellBuildSystem

from build_system.config_android import android_config
from build_system.config_linux import linux_config
from build_system.config_macos import macos_config
from build_system.config_win32 import win32_config

build_step_sequence = [
    c.build_node_js,
    c.build_j2v8_cmake,
    c.build_j2v8_jni,
    c.build_j2v8_java,
]

avail_targets = {
    c.target_android: android_config,
    c.target_linux: linux_config,
    c.target_macos: macos_config,
    c.target_win32: win32_config,
}

avail_build_steps = [
    c.build_node_js,
    c.build_j2v8_cmake,
    c.build_j2v8_jni,
    c.build_j2v8_java,
    c.build_all,
    c.build_full,
    c.build_native,
    c.build_java,
]

#-----------------------------------------------------------------------
# Command-Line setup
#-----------------------------------------------------------------------

parser = argparse.ArgumentParser()

parser.add_argument('--target', '-t',
                    dest='target',
                    choices=[
                        c.target_android,
                        c.target_linux,
                        c.target_macos,
                        c.target_win32,
                    ])

parser.add_argument('--arch', '-a',
                    dest='arch',
                    choices=[
                        c.arch_x86,
                        c.arch_x64,
                        c.arch_arm,
                    ])

parser.add_argument('--cross-compile', '-x',
                    dest='cross_compile',
                    action='store_const',
                    const=True)

parser.add_argument('--node-enabled', '-ne',
                    dest='node_enabled',
                    action='store_const',
                    const=True)

parser.add_argument('buildsteps',
                    metavar='build-step',
                    nargs='*',
                    default='all',
                    choices=avail_build_steps)

buildsteps = set()

def parse_build_step_option(step):
    return {
        c.build_all: add_all,
        c.build_full: add_all,
        c.build_native: add_native,
        c.build_java: add_managed,
        c.build_node_js: lambda: buildsteps.add(c.build_node_js),
        c.build_j2v8_cmake: lambda: buildsteps.add(c.build_j2v8_cmake),
        c.build_j2v8_jni: lambda: buildsteps.add(c.build_j2v8_jni),
        c.build_j2v8_java: lambda: buildsteps.add(c.build_j2v8_java),
    }.get(step, raise_unhandled_option)

def add_all():
    add_native()
    add_managed()

def add_native():
    buildsteps.add(c.build_node_js)
    buildsteps.add(c.build_j2v8_cmake)
    buildsteps.add(c.build_j2v8_jni)

def add_managed():
    buildsteps.add(c.build_j2v8_java)

def raise_unhandled_option():
    sys.exit("INTERNAL-ERROR: Tried to handle unrecognized build-step")

args = parser.parse_args()

#-----------------------------------------------------------------------
# Build execution core function
#-----------------------------------------------------------------------

def execute_build(target, arch, steps, node_enabled = True, cross_compile = False):

    if (target is None):
        sys.exit("ERROR: No target platform specified, use --target <...>")

    if (not target in avail_targets):
        sys.exit("ERROR: Unrecognized target platform: " + target)

    build_target = avail_targets.get(target)

    if (arch is None):
        sys.exit("ERROR: No target architecture specified, use --arch <...>")

    build_architectures = build_target.architectures

    if (not arch in build_architectures):
        sys.exit("ERROR: Unsupported architecture: \"" + arch + "\" for selected target platform: " + target)

    if (steps is None):
        sys.exit("ERROR: No build-step specified, valid values are: " + ", ".join(avail_build_steps))

    if (not steps is None and not isinstance(steps, list)):
        steps = [steps]

    global buildsteps
    buildsteps.clear()

    for step in steps:
        parse_build_step_option(step)()

    # force build-steps into defined order (see: http://stackoverflow.com/a/23529016)
    buildsteps = [step for step in build_step_sequence if step in buildsteps]

    configs = build_target.configs

    if (cross_compile):
        x_compiler = build_target.cross_compiler()
        x_config = configs.get('cross')
        x_cmd = "python ./build.py -t $PLATFORM -a $ARCH " + ("-ne" if node_enabled else "") + " " + " ".join(buildsteps)
        x_compiler.build(x_config, arch, x_cmd)
    else:
        # TODO: get native build system Batch vs Shell
        host_compiler = ShellBuildSystem()
        host_configs = dict(configs)
        del host_configs['cross']
        # build all requested build steps
        [host_compiler.build(host_configs[step], arch) for step in buildsteps]

if __name__ == '__main__':
    execute_build(args.target, args.arch, args.buildsteps, args.node_enabled, args.cross_compile)
