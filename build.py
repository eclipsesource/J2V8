import argparse
import collections
import os
import re
import sys

import build_system.constants as c
import build_system.build_utils as utils
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
    c.build_j2v8_junit,
]

avail_targets = {
    c.target_android: android_config,
    c.target_linux: linux_config,
    c.target_macos: macos_config,
    c.target_win32: win32_config,
}

avail_build_steps = [
    # detail steps
    c.build_node_js,
    c.build_j2v8_cmake,
    c.build_j2v8_jni,
    c.build_j2v8_java,
    c.build_j2v8_junit,

    # aggregate steps
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
        c.build_j2v8_junit: lambda: buildsteps.add(c.build_j2v8_junit),
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
# Sanity check for the builtin node-module links in J2V8 C++ JNI code
#-----------------------------------------------------------------------
def check_node_builtins():
    j2v8_jni_cpp_path = "jni/com_eclipsesource_v8_V8Impl.cpp"
    j2v8_builtins = []

    with open(j2v8_jni_cpp_path, 'r') as j2v8_jni_cpp:
        j2v8_code = j2v8_jni_cpp.read()

    tag = "// @node-builtins-force-link"
    start = j2v8_code.find(tag)

    end1 = j2v8_code.find("}", start)
    end2 = j2v8_code.find("#endif", start)

    if (end1 < 0 and end2 < 0):
        return

    end = min(int(e) for e in [end1, end2])

    if (end < 0):
        return

    j2v8_linked_builtins = j2v8_code[start + len(tag):end]

    j2v8_builtins = [m for m in re.finditer(r"^\s*_register_(?P<name>.+)\(\);\s*$", j2v8_linked_builtins, re.M)]

    comment_tokens = ["//", "/*", "*/"]

    j2v8_builtins = [x.group("name") for x in j2v8_builtins if not any(c in x.group(0) for c in comment_tokens)]

    node_src = "node/src/"
    node_builtins = []
    for cc_file in os.listdir(node_src):
        if (not cc_file.endswith(".cc")):
            continue

        with open(node_src + cc_file, 'r') as node_cpp:
            node_code = node_cpp.read()

        m = re.search(r"NODE_MODULE_CONTEXT_AWARE_BUILTIN\((.*),\s*node::.*\)", node_code)

        if (m is not None):
            node_builtins.append(m.group(1))

    # are all Node.js builtins mentioned?
    builtins_ok = collections.Counter(j2v8_builtins) == collections.Counter(node_builtins)

    if (not builtins_ok):
        sys.exit("ERROR: J2V8 linking builtins code does not match Node.js builtin modules, check " + j2v8_jni_cpp_path)

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
        # TODO: move pre-build checks / steps to main program and run it only in the real program instigator (helps performance & early build abort in error cases)
        # pre-build sanity checks
        check_node_builtins()

        # TODO: get native build system Batch vs Shell
        host_compiler = ShellBuildSystem()
        host_configs = dict(configs)

        # TODO: use a central / single / immutable source of truth for the CWD
        build_cwd = os.getcwd().replace("\\", "/")

        if (host_configs.has_key('cross')):
            x_config = host_configs.get('cross')
            build_cwd = x_config.build_cwd
            del host_configs['cross']

        # build all requested build steps
        for step in buildsteps:
            h_config = host_configs[step]

            # TODO: move pre-build checks / steps to main program and run it only in the real program instigator (helps performance & early build abort in error cases)
            # if we build Node.js then save any potentially existing build artifacts from a different platform
            if (step == c.build_node_js):
                utils.store_nodejs_output(h_config, arch, build_cwd)

            host_compiler.build(h_config, arch)

# check if this script was invoked via CLI directly to start a build
if __name__ == '__main__':
    execute_build(args.target, args.arch, args.buildsteps, args.node_enabled, args.cross_compile)
