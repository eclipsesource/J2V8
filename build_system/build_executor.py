
import sys

from . import cli
from . import build_constants as bc
from . import constants as c
from . import build_utils as utils
from .shell_build import ShellBuildSystem
from . import immutable

class BuildState:
    # collection of all parsed build-steps that will then be passed on to the core build function
    # (this list must only contain atomic steps after all step evaluations are finished)
    parsed_steps = set()

    # a registry/dictionary of evaluation-functions that translate from their corresponding step/alias
    # into the list of atomic build-steps (see parsed_steps above)
    step_evaluators = {}

#-----------------------------------------------------------------------
# Advanced build-step parsing (anti-steps, multi-steps)
#-----------------------------------------------------------------------

def atomic_step(step, alias = None):
    """
    Atomic build-steps are just directly forwarded to the build-executor.
    This function will also automatically add an additional anti-step with a "~" prefix.
    """
    if (alias is None):
        alias = step

    step_eval = BuildState.step_evaluators
    parsed_steps = BuildState.parsed_steps

    # add step handler (step => step)
    step_eval[alias] = lambda: parsed_steps.add(step)

    # add anti-step handler (step => ~step)
    step_eval["~" + alias] = lambda: parsed_steps.discard(step)

    # register additional anti-step in CLI
    bc.avail_build_steps.append("~" + alias)

def multi_step(alias, include, exclude = []):
    """
    Forwards a collection/sequence of build-steps to the build-executor when
    the defined step alias name was detected. Also the inverted anti-steps sequence
    will be evaluated if the "~" prefixed alias is recognized.
    """

    step_eval = BuildState.step_evaluators

    # add aliased step-sequence (alias => step1, step2, ... , stepN)
    step_eval[alias] = lambda: \
        [step_eval.get(s)() for s in include] + \
        [step_eval.get("~" + s)() for s in exclude]

    # add aliased anti-step-sequence (~alias => ~step1, ~step2, ... , ~stepN)
    step_eval["~" + alias] = lambda: \
        [step_eval.get("~" + s)() for s in include] + \
        [step_eval.get(s)() for s in exclude]

    # register additional anti-step in CLI
    bc.avail_build_steps.append("~" + alias)

def init_buildsteps():
    """Setup of all available build-step atomics & combinations"""
    # special alias to group all build steps into a single one
    multi_step(c.build_all, [
        c.build_v8,
        c.build_j2v8_cmake,
        c.build_j2v8_jni,
        c.build_j2v8_cpp,
        c.build_j2v8_optimize,
        c.build_j2v8_java,
        c.build_j2v8_test,
    ])

    # atomic steps
    for step in list(bc.atomic_build_step_sequence):
        atomic_step(step)

    # atomic aliases
    atomic_step(c.build_j2v8_java, c.build_java)
    atomic_step(c.build_j2v8_test, c.build_test)

    # multi-step alias: build only the native parts (includes V8)
    multi_step(c.build_native, [
        c.build_v8,
        c.build_j2v8_cmake,
        c.build_j2v8_jni,
        c.build_j2v8_cpp,
        c.build_j2v8_optimize,
    ])

    # multi-step alias: build everything that belongs to J2V8 (excludes V8)
    # this is useful when building J2V8 with a pre-compiled V8 dependency package
    multi_step(c.build_j2v8, [c.build_all], [c.build_v8, c.build_j2v8_test])

def evaluate_build_step_option(step):
    """Find the registered evaluator function for the given step and execute it"""
    step_eval_func = BuildState.step_evaluators.get(step, raise_unhandled_option(step))
    step_eval_func()

def raise_unhandled_option(step):
    return lambda: utils.cli_exit("INTERNAL-ERROR: Tried to handle unrecognized build-step \"" + step + "\"")

# initialize the advanced parsing evaluation handlers for the build.py CLI
init_buildsteps()

#-----------------------------------------------------------------------
# Build execution core function
#-----------------------------------------------------------------------
def execute_build(params):
    """
    Receives an params-object with all the necessary build-settings to start
    building the J2V8 artifacts. There are two paths internally that this function will take:

    A) Run the build in the same OS shell environment that the build.py command was started from.
    This means you have to make sure all the necessary build utensils are installed on your system.
    To find out what is needed to build on a particular platform you can have a look in the "docker"
    and "vagrant" directories, they contain shell scripts that show how to install all the things
    you need if you would want to set up a build environment manually on your machine.

    B) Use virtualization technologies to run a sandboxed build-environment that does not rely
    on your machine having installed any of the required build-tools natively. This also allows
    to cross-compile mostly all supported platforms independently of the host operating system that
    you are running on your machine (only Docker and/or Vagrant are required to run this).
    """
    # convert from a dictionary form to the normalized params-object form
    if (isinstance(params, dict)):
        params = cli.BuildParams(params)

    # can be used to force output of all started sub-processes through the host-process stdout
    utils.redirect_stdout_enabled = hasattr(params, "redirect_stdout") and params.redirect_stdout

    if (params.target is None):
        utils.cli_exit("ERROR: No target platform specified")

    if (params.docker and params.vagrant):
        utils.cli_exit("ERROR: Choose either Docker or Vagrant for the build, can not use both")

    target = params.target

    if (not target in bc.platform_configs):
        utils.cli_exit("ERROR: Unrecognized target platform: " + target)

    # this defines the PlatformConfig / operating system the build should be run for
    target_platform = bc.platform_configs.get(target)

    if (params.arch is None):
        utils.cli_exit("ERROR: No target architecture specified")

    avail_architectures = target_platform.architectures

    if (not params.arch in avail_architectures):
        utils.cli_exit("ERROR: Unsupported architecture: \"" + params.arch + "\" for selected target platform: " + target)

    if (params.buildsteps is None):
        utils.cli_exit("ERROR: No build-step specified, valid values are: " + ", ".join(bc.avail_build_steps))

    if (not params.buildsteps is None and not isinstance(params.buildsteps, list)):
        params.buildsteps = [params.buildsteps]

    parsed_steps = BuildState.parsed_steps
    parsed_steps.clear()

    # first look for the advanced form of build-step where it might be specified with some arguments to be passed
    # to the underlying build-tool (e.g. --j2v8test="-Dtest=NodeJSTest")
    for step in bc.atomic_build_step_sequence:
        step_args = getattr(params, step, None)

        if step_args:
            parsed_steps.add(step)

    # if there were no special build-step args or atomic build-step args passed
    # then fall back to the default behavior and run all known steps
    if not any(parsed_steps) and not any(params.buildsteps):
        params.buildsteps = ["all"]

    # then go through the raw list of basic build-steps (given by the CLI or an API call)
    # and generate a list of only the atomic build-steps that were derived in the evaluation
    for step in params.buildsteps:
        evaluate_build_step_option(step)

    # force build-steps into their pre-defined order (see: http://stackoverflow.com/a/23529016)
    parsed_steps = [step for step in bc.atomic_build_step_sequence if step in parsed_steps]

    if (len(parsed_steps) == 0):
        utils.cli_exit("WARNING: No build-steps to be done ... exiting")

    build_cwd = utils.get_cwd()

    cross_cfg = None
    cross_configs = target_platform.cross_configs

    cross_sys = "docker" if params.docker else "vagrant" if params.vagrant else None

    # if a recognized cross-compile option was specified by the params
    # try to find the configuration parameters to run the cross-compiler
    if (cross_sys):
        if (cross_configs.get(cross_sys) is None):
            utils.cli_exit("ERROR: target '" + target + "' does not have a recognized cross-compile host: '" + cross_sys + "'")
        else:
            cross_cfg = cross_configs.get(cross_sys)

    # if we are the build-instigator (not a cross-compile build-agent) we directly run some initial checks & setups for the build
    # if (not params.cross_agent):
        # print "Checking V8 builtins integration consistency..."
        # utils.check_node_builtins()

        # v8_major,v8_minor,v8_build,v8_patch,v8_is_candidate = utils.get_v8_version()

        # print "--------------------------------------------------"
        # print "V8:      %(v8_major)s.%(v8_minor)s.%(v8_build)s.%(v8_patch)s (candidate: %(v8_is_candidate)s)" % locals()
        # print "--------------------------------------------------"

        # print "Caching V8 artifacts..."
        # curr_node_tag = (params.vendor + "-" if params.vendor else "") + target + "." + params.arch
        # utils.store_nodejs_output(curr_node_tag, build_cwd)

    def execute_build_step(build_system, build_step, v8_build = False):
        """Creates an immutable copy of a single BuildStep configuration and executes it in the build-system"""
        # from this point on, make the build-input immutable to ensure consistency across the whole build process
        # any actions during the build-step should only be made based on the initial set of variables & conditions
        # NOTE: this restriction makes it much more easy to reason about the build-process as a whole (see "unidirectional data flow")
        build_step = immutable.freeze(build_step)
        if (v8_build):
            build_system.build_v8(build_step)
        else:    
            build_system.build(build_step)

    # a cross-compile was requested, we just launch the virtualization-environment and then delegate
    # the originally requested build parameters to the cross-compile environment then running the build.py CLI
    if (cross_cfg):
        cross_compiler = target_platform.cross_compiler(cross_sys)

        parsed_step_args = ""

        # look for build-step arguments that were passed in by the user
        # e.g. --j2v8test="-Dtest=..." and pass them down to the cross-agent also
        for step in bc.atomic_build_step_sequence:
            step_args = getattr(params, step, None)

            if step_args:
                parsed_step_args += " --" + step + "='" + step_args + "'"

        # invoke the build.py CLI within the virtualized / self-contained build-system provider
        cross_cfg.custom_cmd = "python ./build.py " + \
            "--cross-agent " + cross_sys + \
            " -t $PLATFORM -a $ARCH " + \
            (" -ne" if params.node_enabled else "") + \
            (" -v " + params.vendor if params.vendor else "") + \
            (" -knl " if params.keep_native_libs else "") + \
            " " + " ".join(parsed_steps) + parsed_step_args

        # apply meta-vars & util functions
        cross_cfg.compiler = cross_compiler
        cross_cfg.inject_env = lambda s: cross_compiler.inject_env(s, cross_cfg)
        cross_cfg.target = target_platform

        # apply essential build params
        cross_cfg.arch = params.arch
        cross_cfg.file_abi = target_platform.file_abi(params.arch)
        cross_cfg.no_shutdown = params.no_shutdown
        cross_cfg.sys_image = params.sys_image
        cross_cfg.vendor = params.vendor
        cross_cfg.docker = params.docker
        cross_cfg.vagrant = params.vagrant

        if 'v8' in parsed_steps:
            parsed_steps.remove('v8')
            
            # first build V8 and store output
            execute_build_step(cross_compiler, cross_cfg, True)

        
        # start the cross-compile
        execute_build_step(cross_compiler, cross_cfg)

    # run the requested build-steps & parameters in the current shell environment
    else:
        target_compiler = ShellBuildSystem()
        build_steps = dict(target_platform.steps)

        # this is a build-agent for a cross-compile
        if (params.cross_agent):
            # the cross-compile step dictates which directory will be used to run the actual build
            cross_cfg = cross_configs.get(params.cross_agent)

            if (cross_cfg is None):
                utils.cli_exit("ERROR: internal error while looking for cross-compiler config: " + params.cross_agent)

            build_cwd = cross_cfg.build_cwd

        # execute all steps from a list that parsed / evaluated before (see the "build-step parsing" section above)
        for step in parsed_steps:
            if (not step in build_steps):
                print("WARNING: skipping build step \"" + step + "\" (not configured and/or supported for platform \"" + params.target + "\")")
                continue

            target_step = build_steps[step]

            # apply meta-vars & util functions
            target_step.cross_agent = params.cross_agent
            target_step.compiler = target_compiler
            target_step.inject_env = lambda s: target_compiler.inject_env(s, build_steps[step])
            target_step.target = target_platform

            # apply essential build params
            target_step.arch = params.arch
            target_step.file_abi = target_platform.file_abi(params.arch)
            target_step.node_enabled = params.node_enabled
            target_step.build_cwd = build_cwd
            target_step.vendor = params.vendor
            target_step.docker = params.docker
            target_step.vagrant = params.vagrant
            target_step.keep_native_libs = params.keep_native_libs
            target_step.args = getattr(params, step, None)

            # run the current BuildStep
            execute_build_step(target_compiler, target_step)
