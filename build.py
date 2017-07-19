import argparse
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

import build_system.immutable as immutable

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
    c.build_full,
    c.build_native,
    # aliases
    c.build_java,
    c.build_bundle,
    c.build_test,
]

avail_targets = {
    c.target_android: android_config,
    c.target_linux: linux_config,
    c.target_macos: macos_config,
    c.target_win32: win32_config,
}

avail_architectures = [
    c.arch_x86,
    c.arch_x64,
    c.arch_arm,
]

avail_build_steps = build_step_sequence + composite_steps

# this goes through all known target platforms, and returns the sub-targets
# that are available for cross-compilation
def get_cross_targets():
    cross_targets = []

    for tgt in avail_targets.values():
        if (not tgt.cross_compilers):
            continue

        for xcomp in tgt.cross_compilers:
            cross_targets.append(tgt.name + ":" + xcomp)

    return cross_targets

#-----------------------------------------------------------------------
# Command-Line setup
#-----------------------------------------------------------------------

parser = argparse.ArgumentParser(formatter_class=argparse.RawTextHelpFormatter)

parser.add_argument("--target", "-t",
                    help="The build target platform name (must be a valid platform string identifier).",
                    dest="target",
                    required=True,
                    choices=sorted(avail_targets.keys() + get_cross_targets()))

parser.add_argument("--arch", "-a",
                    help="The build target architecture identifier (the available architectures are also dependent on the selected platform for a build).",
                    dest="arch",
                    required=True,
                    choices=avail_architectures)

parser.add_argument("--node-enabled", "-ne",
                    help="Include the Node.js runtime and builtin node-modules for use in J2V8.",
                    dest="node_enabled",
                    action="store_const",
                    const=True)

# NOTE: this option is only used internally to distinguish the running of the build script within
# the build-instigator and the actual build-executor (this is relevant when cross-compiling)
parser.add_argument("--cross-agent",
                    help=argparse.SUPPRESS,
                    dest="cross_agent",
                    type=str)

parser.add_argument("--no-shutdown", "-nos",
                    help="When using a cross-compile environment, do not shutdown any of the components when the build is finished or canceled.",
                    dest="no_shutdown",
                    action="store_const",
                    const=True)

parser.add_argument("buildsteps",
                    help="Pass a single build-step or a list of all the recognized build-steps that should be executed\n" +
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
        c.build_bundle: add_managed,
        c.build_test: add_test,
        # basic steps
        c.build_node_js: lambda: parsed_steps.add(c.build_node_js),
        c.build_j2v8_cmake: lambda: parsed_steps.add(c.build_j2v8_cmake),
        c.build_j2v8_jni: lambda: parsed_steps.add(c.build_j2v8_jni),
        c.build_j2v8_optimize: lambda: parsed_steps.add(c.build_j2v8_optimize),
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
    parsed_steps.add(c.build_j2v8_optimize)

def add_managed():
    parsed_steps.add(c.build_j2v8_java)

def add_test():
    parsed_steps.add(c.build_j2v8_junit)

def raise_unhandled_option():
    sys.exit("INTERNAL-ERROR: Tried to handle unrecognized build-step")

if __name__ == "__main__":
    args = parser.parse_args()

#-----------------------------------------------------------------------
# Build execution core function
#-----------------------------------------------------------------------
def execute_build(params):

    if (params.target is None):
        sys.exit("ERROR: No target platform specified")

    def parse_target(target_str):
        sep_idx = target_str.find(":")
        return (target_str, None) if sep_idx < 0 else target_str[0:sep_idx], target_str[sep_idx+1:]

    # if the "target" string {x:y} passed to the CLI exactly identifies a build-target, we just take it and continue.
    # This means that if you want to introduce a customized build for a platform named {platform:custom-name},
    # it will be picked up before any further deconstruction of a "target:sub-target" string is done
    build_target = avail_targets.get(params.target)

    target = None
    cross_id = None

    # if the passed "target" string is not already a valid build-target, we need to look for sub-targets
    if (build_target is None):
        target, cross_id = parse_target(params.target)
    # otherwise we just go on with it
    else:
        target = params.target

    if (not target in avail_targets):
        sys.exit("ERROR: Unrecognized target platform: " + target)

    build_target = avail_targets.get(target)

    if (params.arch is None):
        sys.exit("ERROR: No target architecture specified")

    build_architectures = build_target.architectures

    if (not params.arch in build_architectures):
        sys.exit("ERROR: Unsupported architecture: \"" + params.arch + "\" for selected target platform: " + target)

    if (params.buildsteps is None):
        sys.exit("ERROR: No build-step specified, valid values are: " + ", ".join(avail_build_steps))

    if (not params.buildsteps is None and not isinstance(params.buildsteps, list)):
        params.buildsteps = [params.buildsteps]

    global parsed_steps
    parsed_steps.clear()

    for step in params.buildsteps:
        parse_build_step_option(step)()

    # force build-steps into defined order (see: http://stackoverflow.com/a/23529016)
    parsed_steps = [step for step in build_step_sequence if step in parsed_steps]

    platform_steps = build_target.steps
    cross_configs = build_target.cross_configs

    build_cwd = utils.get_cwd()

    cross_cfg = None

    if (cross_id):
        if (cross_configs.get(cross_id) is None):
            sys.exit("ERROR: target '" + target + "' does not have a recognized cross-compile host: '" + cross_id + "'")
        else:
            cross_cfg = cross_configs.get(cross_id)

    # if we are the build-instigator (not a cross-compile build-agent) we directly run some initial checks & setups for the build
    if (not params.cross_agent):
        print "Checking Node.js builtins integration consistency..."
        utils.check_node_builtins()

        print "Caching Node.js artifacts..."
        curr_node_tag = target + "." + params.arch
        utils.store_nodejs_output(curr_node_tag, build_cwd)

    def execute_build_step(compiler_inst, build_step):
        """Executes an immutable copy of the given build-step configuration"""
        # from this point on, make the build-input immutable to ensure consistency across the whole build process
        # any actions during the build-step should only be made based on the initial set of variables & conditions
        # NOTE: this restriction makes it much more easy to reason about the build-process as a whole
        build_step = immutable.freeze(build_step)
        compiler_inst.build(build_step)

    # a cross-compile was requested, we just launch the build-environment and then delegate the requested build-process to the cross-compile environment
    if (cross_cfg):
        cross_compiler = build_target.cross_compiler(cross_id)

        # prepare any additional/dynamic parameters for the build and put them into the build-step config
        cross_cfg.arch = params.arch
        cross_cfg.custom_cmd = "python ./build.py --cross-agent " + cross_id + " -t $PLATFORM -a $ARCH " + ("-ne" if params.node_enabled else "") + " " + " ".join(parsed_steps)
        cross_cfg.compiler = cross_compiler
        cross_cfg.target = build_target
        cross_cfg.no_shutdown = params.no_shutdown

        execute_build_step(cross_compiler, cross_cfg)

    # run the requested build-steps with the given parameters to produce the build-artifacts
    else:
        target_compiler = ShellBuildSystem()
        target_steps = dict(platform_steps)

        # this is a build-agent for a cross-compile
        if (params.cross_agent):
            # the cross-compile step dictates which directory will be used to run the actual build
            cross_cfg = cross_configs.get(params.cross_agent)

            if (cross_cfg is None):
                sys.exit("ERROR: internal error while looking for cross-compiler config: " + params.cross_agent)

            build_cwd = cross_cfg.build_cwd

        # execute all requested build steps
        for step in parsed_steps:
            if (not step in target_steps):
                sys.exit("Hint: skipping build step \"" + step + "\" (not configured and/or supported for platform \"" + params.target + "\")")
                continue

            target_step = target_steps[step]

            # prepare any additional/dynamic parameters for the build and put them into the build-step config
            target_step.cross_agent = params.cross_agent
            target_step.arch = params.arch
            target_step.build_cwd = build_cwd
            target_step.compiler = target_compiler
            target_step.target = build_target

            execute_build_step(target_compiler, target_step)

# check if this script was invoked via CLI directly to start a build
if __name__ == "__main__":
    execute_build(args)
