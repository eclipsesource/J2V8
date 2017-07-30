
import sys

import cli
import build_constants as bc
import constants as c
import build_utils as utils
from shell_build import ShellBuildSystem

import immutable

parsed_steps = set()

step_handlers = {}

def atomic_step(step, alias = None):
    if (alias is None):
        alias = step

    # handle anti-step
    step_handlers[alias] = lambda: parsed_steps.add(step)

    # handle anti-step
    step_handlers["~" + alias] = lambda: parsed_steps.discard(step)

    # register anti-step in CLI
    bc.avail_build_steps.append("~" + alias)

def multi_step(alias, include, exclude = []):
    # handle step
    step_handlers[alias] = lambda: \
        [step_handlers.get(s)() for s in include] + \
        [step_handlers.get("~" + s)() for s in exclude]

    # handle anti-step
    step_handlers["~" + alias] = lambda: \
        [step_handlers.get("~" + s)() for s in include] + \
        [step_handlers.get(s)() for s in exclude]

    # register anti-step in CLI
    bc.avail_build_steps.append("~" + alias)

def init_buildsteps():
    # special alias to include all build steps into one
    multi_step(c.build_all, bc.build_step_sequence)

    # atomic steps
    for step in list(bc.build_step_sequence):
        atomic_step(step)

    # atomic aliases
    atomic_step(c.build_j2v8_java, c.build_java)
    atomic_step(c.build_j2v8_junit, c.build_test)

    # composite alias: build only the native parts (including nodejs)
    multi_step(c.build_native, [
        c.build_node_js,
        c.build_j2v8_cmake,
        c.build_j2v8_jni,
        c.build_j2v8_optimize,
    ])

    # composite alias: build everything except nodejs
    multi_step(c.build_j2v8, [c.build_all], [c.build_node_js])

def handle_build_step_option(step):
    return step_handlers.get(step, raise_unhandled_option(step))

def raise_unhandled_option(step):
    return lambda: sys.exit("INTERNAL-ERROR: Tried to handle unrecognized build-step \"" + step + "\"")

# initialize the advanced parsing mechanisms for the build CLI
init_buildsteps()

#-----------------------------------------------------------------------
# Build execution core function
#-----------------------------------------------------------------------
def execute_build(params):

    # if (type(params) is dict):
    if (isinstance(params, dict)):
        params = cli.BuildParams(params)

    if (params.target is None):
        sys.exit("ERROR: No target platform specified")

    if (params.docker and params.vagrant):
        sys.exit("ERROR: Choose either Docker or Vagrant for the build, can not use both")

    # this defines the target platform / operating system the build should be run for
    build_target = bc.platform_targets.get(params.target)

    target = params.target
    cross_id = "docker" if params.docker else "vagrant" if params.vagrant else None

    if (not target in bc.platform_targets):
        sys.exit("ERROR: Unrecognized target platform: " + target)

    build_target = bc.platform_targets.get(target)

    if (params.arch is None):
        sys.exit("ERROR: No target architecture specified")

    build_architectures = build_target.architectures

    if (not params.arch in build_architectures):
        sys.exit("ERROR: Unsupported architecture: \"" + params.arch + "\" for selected target platform: " + target)

    if (params.buildsteps is None):
        sys.exit("ERROR: No build-step specified, valid values are: " + ", ".join(bc.avail_build_steps))

    if (not params.buildsteps is None and not isinstance(params.buildsteps, list)):
        params.buildsteps = [params.buildsteps]

    global parsed_steps
    parsed_steps.clear()

    for step in params.buildsteps:
        handle_build_step_option(step)()

    # force build-steps into defined order (see: http://stackoverflow.com/a/23529016)
    parsed_steps = [step for step in bc.build_step_sequence if step in parsed_steps]

    if (len(parsed_steps) == 0):
        sys.exit("WARNING: No build-steps to be done ... exiting")

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
        curr_node_tag = (params.vendor + "-" if params.vendor else "") + target + "." + params.arch
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

        # prepare additional parameters/utils for the build and put them into the build-step config

        cross_cfg.custom_cmd = "python ./build.py " + \
            "--cross-agent " + cross_id + \
            " -t $PLATFORM -a $ARCH " + \
            (" -ne" if params.node_enabled else "") + \
            (" -v " + params.vendor if params.vendor else "") + \
            (" -knl " if params.keep_native_libs else "") + \
            " " + " ".join(parsed_steps)

        # meta-vars & util functions
        cross_cfg.compiler = cross_compiler
        cross_cfg.inject_env = lambda s: cross_compiler.inject_env(s, cross_cfg)
        cross_cfg.target = build_target

        # build params
        cross_cfg.arch = params.arch
        cross_cfg.file_abi = build_target.file_abi(params.arch)
        cross_cfg.no_shutdown = params.no_shutdown
        cross_cfg.sys_image = params.sys_image
        cross_cfg.vendor = params.vendor
        cross_cfg.docker = params.docker
        cross_cfg.vagrant = params.vagrant

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
                print("INFO: skipping build step \"" + step + "\" (not configured and/or supported for platform \"" + params.target + "\")")
                continue

            target_step = target_steps[step]

            # prepare additional parameters/utils for the build and put them into the build-step config

            # meta-vars & util functions
            target_step.cross_agent = params.cross_agent
            target_step.compiler = target_compiler
            target_step.inject_env = lambda s: target_compiler.inject_env(s, target_steps[step])
            target_step.target = build_target

            # build params
            target_step.arch = params.arch
            target_step.file_abi = build_target.file_abi(params.arch)
            target_step.node_enabled = params.node_enabled
            target_step.build_cwd = build_cwd
            target_step.vendor = params.vendor
            target_step.docker = params.docker
            target_step.vagrant = params.vagrant
            target_step.keep_native_libs = params.keep_native_libs

            execute_build_step(target_compiler, target_step)
