import argparse
import sys
import build_constants as bc
from shell_build import ShellBuildSystem

from config_android import android_config
from config_linux import linux_config
from config_macos import macos_config
from config_win32 import win32_config

build_step_sequence = [
    bc.build_node_js,
    bc.build_j2v8_jni,
    bc.build_j2v8_java,
]

avail_targets = {
    bc.target_android: android_config,
    bc.target_linux: linux_config,
    bc.target_macos: macos_config,
    bc.target_win32: win32_config,
}

avail_build_steps = [
    bc.build_node_js,
    bc.build_j2v8_jni,
    bc.build_j2v8_java,
    bc.build_all,
    bc.build_full,
    bc.build_native,
    bc.build_java,
]

#-----------------------------------------------------------------------
# Command-Line setup
#-----------------------------------------------------------------------

parser = argparse.ArgumentParser()

parser.add_argument('--target', '-t',
                    dest='target',
                    choices=[
                        bc.target_android,
                        bc.target_linux,
                        bc.target_macos,
                        bc.target_win32,
                    ])

parser.add_argument('--arch', '-a',
                    dest='arch',
                    choices=[
                        bc.arch_x86,
                        bc.arch_x64,
                        bc.arch_arm,
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
        bc.build_all: add_all,
        bc.build_full: add_all,
        bc.build_native: add_native,
        bc.build_java: add_managed,
        bc.build_node_js: lambda: buildsteps.add(bc.build_node_js),
        bc.build_j2v8_jni: lambda: buildsteps.add(bc.build_j2v8_jni),
        bc.build_j2v8_java: lambda: buildsteps.add(bc.build_j2v8_java),
    }.get(step, raise_unhandled_option)

def add_all():
    add_native()
    add_managed()

def add_native():
    buildsteps.add(bc.build_node_js)
    buildsteps.add(bc.build_j2v8_jni)

def add_managed():
    buildsteps.add(bc.build_j2v8_java)

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
