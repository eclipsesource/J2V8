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
from build_system.config_alpine import alpine_config
from build_system.config_macos import macos_config
from build_system.config_win32 import win32_config

import build_system.immutable as immutable

build_step_sequence = [
    c.build_node_js,
    c.build_j2v8_cmake,
    c.build_j2v8_jni,
    c.build_j2v8_java,
    c.build_j2v8_junit,
]

composite_steps = [
    c.build_all,
    c.build_full,
    c.build_native,
    c.build_java,
    c.build_test,
]

avail_targets = {
    c.target_android: android_config,
    c.target_linux: linux_config,
    c.target_alpine: alpine_config,
    c.target_macos: macos_config,
    c.target_win32: win32_config,
}

avail_build_steps = build_step_sequence + composite_steps

#-----------------------------------------------------------------------
# Command-Line setup
#-----------------------------------------------------------------------

parser = argparse.ArgumentParser(formatter_class=argparse.RawTextHelpFormatter)

parser.add_argument("--target", "-t",
                    help="The build target platform name (must be a valid platform string identifier).",
                    dest="target",
                    required=True,
                    choices=[
                        c.target_android,
                        c.target_linux,
                        c.target_alpine,
                        c.target_macos,
                        c.target_win32,
                    ])

parser.add_argument("--arch", "-a",
                    help="The build target architecture identifier (the available architectures are also dependent on the selected platform for a build).",
                    dest="arch",
                    required=True,
                    choices=[
                        c.arch_x86,
                        c.arch_x64,
                        c.arch_arm,
                    ])

parser.add_argument("--cross-compile", "-x",
                    help="Run the actual build in a virtualized sandbox environment, fully decoupled from the build host machine.",
                    dest="cross_compile",
                    action="store_const",
                    const=True)

parser.add_argument("--node-enabled", "-ne",
                    help="Include the Node.js runtime and builtin node-modules for use in J2V8.",
                    dest="node_enabled",
                    action="store_const",
                    const=True)

# NOTE: this option is only used internally to distinguish the running of the build script within
# the build-instigator and the actual build-executor (this is relevant when cross-compiling)
parser.add_argument("--build-agent", "-bd",
                    help=argparse.SUPPRESS,
                    dest="build_agent",
                    action="store_const",
                    const=True)

parser.add_argument("buildsteps",
                    help="A single build-step or a list of all the recognized build-steps that should be executed\n" +
                        "(the order of the steps given to the CLI does not matter, the correct order will be restored internally).\n\n" +
                        "the fundamental build steps (in order):\n" +
                        "---------------------------------------\n" +
                        "\n".join(build_step_sequence) + "\n\n" +
                        "aliases / combinations of multiple of the above steps:\n" +
                        "------------------------------------------------------\n" +
                        "\n".join(composite_steps),
                    metavar="build-steps",
                    nargs="*",
                    default="all",
                    choices=avail_build_steps)

parsed_steps = set()

def parse_build_step_option(step):
    return {
        # composite steps
        c.build_all: add_all,
        c.build_full: add_all,
        c.build_native: add_native,
        c.build_java: add_managed,
        c.build_test: add_test,
        # basic steps
        c.build_node_js: lambda: parsed_steps.add(c.build_node_js),
        c.build_j2v8_cmake: lambda: parsed_steps.add(c.build_j2v8_cmake),
        c.build_j2v8_jni: lambda: parsed_steps.add(c.build_j2v8_jni),
        c.build_j2v8_java: lambda: parsed_steps.add(c.build_j2v8_java),
        c.build_j2v8_junit: lambda: parsed_steps.add(c.build_j2v8_junit),
    }.get(step, raise_unhandled_option)

def add_all():
    add_native()
    add_managed()

def add_native():
    parsed_steps.add(c.build_node_js)
    parsed_steps.add(c.build_j2v8_cmake)
    parsed_steps.add(c.build_j2v8_jni)

def add_managed():
    parsed_steps.add(c.build_j2v8_java)

def add_test():
    parsed_steps.add(c.build_j2v8_junit)

def raise_unhandled_option():
    sys.exit("INTERNAL-ERROR: Tried to handle unrecognized build-step")

args = parser.parse_args()

#-----------------------------------------------------------------------
# Sanity check for the builtin node-module links in J2V8 C++ JNI code
#-----------------------------------------------------------------------
def check_node_builtins():
    j2v8_jni_cpp_path = "jni/com_eclipsesource_v8_V8Impl.cpp"
    j2v8_builtins = []

    with open(j2v8_jni_cpp_path, "r") as j2v8_jni_cpp:
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

        with open(node_src + cc_file, "r") as node_cpp:
            node_code = node_cpp.read()

        m = re.search(r"NODE_MODULE_CONTEXT_AWARE_BUILTIN\((.*),\s*node::.*\)", node_code)

        if (m is not None):
            node_builtins.append(m.group(1))

    # are all Node.js builtins mentioned?
    builtins_ok = collections.Counter(j2v8_builtins) == collections.Counter(node_builtins)

    if (not builtins_ok):
        j2v8_extra = [item for item in j2v8_builtins if item not in node_builtins]
        j2v8_missing = [item for item in node_builtins if item not in j2v8_builtins]

        error = "ERROR: J2V8 linking builtins code does not match Node.js builtin modules, check " + j2v8_jni_cpp_path

        if (len(j2v8_extra) > 0):
            error += "\n\t" + "J2V8 defines unrecognized node-modules: " + str(j2v8_extra)

        if (len(j2v8_missing) > 0):
            error += "\n\t" + "J2V8 definition is missing node-modules: " + str(j2v8_missing)

        sys.exit(error)

#-----------------------------------------------------------------------
# Build execution core function
#-----------------------------------------------------------------------
def execute_build(params):

    if (params.target is None):
        sys.exit("ERROR: No target platform specified")

    if (not params.target in avail_targets):
        sys.exit("ERROR: Unrecognized target platform: " + params.target)

    build_target = avail_targets.get(params.target)

    if (params.arch is None):
        sys.exit("ERROR: No target architecture specified")

    build_architectures = build_target.architectures

    if (not params.arch in build_architectures):
        sys.exit("ERROR: Unsupported architecture: \"" + params.arch + "\" for selected target platform: " + params.target)

    if (params.buildsteps is None):
        sys.exit("ERROR: No build-step specified, valid values are: " + ", ".join(avail_build_steps))

    if (not params.buildsteps is None and not isinstance(params.buildsteps, list)):
        params.buildsteps = [params.buildsteps]

    # apply default values for unspecified params
    params.build_agent = params.build_agent if (hasattr(params, "build_agent")) else None

    global parsed_steps
    parsed_steps.clear()

    for step in params.buildsteps:
        parse_build_step_option(step)()

    # force build-steps into defined order (see: http://stackoverflow.com/a/23529016)
    parsed_steps = [step for step in build_step_sequence if step in parsed_steps]

    platform_steps = build_target.steps

    build_cwd = utils.get_cwd()

    if (platform_steps.get("cross") is None):
        sys.exit("ERROR: cross-compilation is not available/supported for platform: " + params.target)

    # if we are the build-instigator (not a cross-compile build-agent) we run some initial checks & setups for the build
    if (hasattr(params, "build_agent") and not params.build_agent):
        print "Checking Node.js builtins integration consistency..."
        check_node_builtins()

        print "Caching Node.js artifacts..."
        curr_node_tag = params.target + "." + params.arch
        utils.store_nodejs_output(curr_node_tag, build_cwd)

    def execute_build_step(compiler, build_step):
        """Executes an immutable copy of the given build-step configuration"""
        # from this point on, make the build-input immutable to ensure consistency across the whole build process
        # any actions during the build-step should only be made based on the initial set of variables & conditions
        # NOTE: this restriction makes it much more easy to reason about the build-process as a whole
        build_step = immutable.freeze(build_step)
        compiler.build(build_step)

    # a cross-compile was requested, we just launch the build-environment and then delegate the requested build-process to the cross-compile environment
    if (params.cross_compile):
        x_compiler = build_target.cross_compiler()
        x_step = platform_steps.get("cross")

        # prepare any additional/dynamic parameters for the build and put them into the build-step config
        x_step.arch = params.arch
        x_step.custom_cmd = "python ./build.py --build-agent -t $PLATFORM -a $ARCH " + ("-ne" if params.node_enabled else "") + " " + " ".join(parsed_steps)
        x_step.compiler = x_compiler
        x_step.target = build_target

        execute_build_step(x_compiler, x_step)

    # run the requested build-steps with the given parameters to produce the build-artifacts
    else:
        target_compiler = ShellBuildSystem()
        target_steps = dict(platform_steps)

        if (target_steps.has_key("cross")):
            x_step = target_steps.get("cross")
            del target_steps["cross"]

            # this is a build-agent for a cross-compile
            if (params.build_agent):
                # the cross-compile step dictates which directory will be used to run the actual build
                build_cwd = x_step.build_cwd

        # execute all requested build steps
        for step in parsed_steps:
            target_step = target_steps[step]

            # prepare any additional/dynamic parameters for the build and put them into the build-step config
            target_step.cross_compile = params.cross_compile
            target_step.build_agent = params.build_agent if (hasattr(params, "build_agent")) else None
            target_step.arch = params.arch
            target_step.build_cwd = build_cwd
            target_step.compiler = target_compiler
            target_step.target = build_target

            execute_build_step(target_compiler, target_step)

# check if this script was invoked via CLI directly to start a build
if __name__ == "__main__":
    execute_build(args)
