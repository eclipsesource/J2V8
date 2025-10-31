import argparse

from . import constants as c
from . import build_constants as bc

class BuildParams(object):
    """Value container for all build-parameters"""

    # essential build CLI parameters
    user_params = {
        "target": None,
        "arch": None,
        "vendor": None,
        "keep_native_libs": None,
        "node_enabled": None,
        "docker": None,
        "vagrant": None,
        "sys_image": None,
        "no_shutdown": None,
        "redirect_stdout": None,
        "buildsteps": c.build_all,
    }

    # additional --buildstep parameters (e.g. --j2v8test)
    step_arg_params = dict((step, None) for step in bc.atomic_build_step_sequence)

    # collection of all known parameters
    known_params = {}
    known_params.update(user_params)
    known_params.update(step_arg_params)

    def __init__(self, param_dict):
        # only the known & accepted parameters will be copied
        # from the input dictionary, to an object-property of the BuildParams object
        known_params = BuildParams.known_params

        unhandled = set(param_dict.keys()).difference(set(known_params.keys()))

        if any(unhandled):
            raise Exception("Unhandled BuildParams: " + str(unhandled))

        for param in known_params:
            # try to read value from input
            value = param_dict.get(param)

            if value != None:
                # use input value
                setattr(self, param, value)
            else:
                # use default value
                default = known_params.get(param)
                setattr(self, param, default)

        # this should never be passed in by the user, it is used just internally
        self.cross_agent = None

def init_args(parser):
    """Initialize all supported build.py parameters and commands on the CLI parser"""
    init_required_args(parser)
    init_optional_args(parser)
    init_feature_args(parser)
    init_cross_compile_args(parser)
    init_meta_args(parser)
    init_build_steps(parser)

def init_required_args(parser):
    #-----------------------------------------------------------------------
    # Essential build settings
    #-----------------------------------------------------------------------
    parser.add_argument("--target", "-t",
                        help="The build target platform name (must be a valid platform string identifier).",
                        dest="target",
                        required=True,
                        choices=sorted(bc.avail_targets))

    parser.add_argument("--arch", "-a",
                        help="The build target architecture identifier (the available architectures are also dependent on the selected platform for a build).",
                        dest="arch",
                        required=True,
                        choices=bc.avail_architectures)

def init_optional_args(parser):
    #-----------------------------------------------------------------------
    # Optional build settings
    #-----------------------------------------------------------------------
    parser.add_argument("--vendor", "-v",
                        help="The operating system vendor (most relevant when building for a specific Linux distribution).",
                        dest="vendor")

    parser.add_argument("--keep-native-libs", "-knl",
                        help="Do not delete the native J2V8 libraries from the Java directories between builds.",
                        dest="keep_native_libs",
                        default=False,
                        action="store_const",
                        const=True)

def init_feature_args(parser):
    #-----------------------------------------------------------------------
    # J2V8 Feature switches
    #-----------------------------------------------------------------------
    parser.add_argument("--node-enabled", "-ne",
                        help="Include the Node.js runtime and builtin node-modules for use in J2V8.",
                        dest="node_enabled",
                        default=False,
                        action="store_const",
                        const=True)

def init_cross_compile_args(parser):
    #-----------------------------------------------------------------------
    # Docker / Vagrant cross-compile settings
    #-----------------------------------------------------------------------
    parser.add_argument("--docker", "-dkr",
                        help="Run a cross-compile environment in a Docker container (all required build-tools are then fully contained & virtualized).",
                        dest="docker",
                        default=False,
                        action="store_const",
                        const=True)

    parser.add_argument("--vagrant", "-vgr",
                        help="Run a cross-compile environment in a Vagrant virtual machine (all required build-tools are then fully contained & virtualized).",
                        dest="vagrant",
                        default=False,
                        action="store_const",
                        const=True)

    parser.add_argument("--sys-image", "-img",
                        help="The operating system image to use as a basis for the virtualized build systems (used in Docker & Vagrant builds).",
                        dest="sys_image")

    parser.add_argument("--no-shutdown", "-nos",
                        help="When using a cross-compile environment, do not shutdown the virtualized environment when the build is finished or canceled.",
                        dest="no_shutdown",
                        action="store_const",
                        const=True)

def init_meta_args(parser):
    #-----------------------------------------------------------------------
    # Meta-Args
    #-----------------------------------------------------------------------
    # NOTE: this option is only used internally to distinguish the running of the build script within
    # the build-instigator and the actual build-executor (this is relevant when cross-compiling)
    parser.add_argument("--cross-agent",
                        help=argparse.SUPPRESS,
                        dest="cross_agent",
                        type=str)

    parser.add_argument("--redirect-stdout", "-rso",
                        help="Make sure that the stdout/stderr of sub-proccesses running shell commands is also going through the " +
                        "output interface of the python host process that is running the build.\n" +
                        "(this is required when running tests for the build-system, without this option the output of the subprocesses will "+
                        "not show up in the test logs)",
                        dest="redirect_stdout",
                        default=False,
                        action="store_const",
                        const=True)

    parser.add_argument("--interactive", "-i",
                    help="Run the interactive version of the J2V8 build CLI.",
                    dest="interactive",
                    default=False,
                    action="store_const",
                    const=True)

def init_build_steps(parser):
    #-----------------------------------------------------------------------
    # Build-Steps
    #-----------------------------------------------------------------------
    parser.add_argument("buildsteps",
                        help="Pass a single build-step or a list of all the recognized build-steps that should be executed\n" +
                            "(the order of the steps given to the CLI does not matter, the correct order will be restored internally).\n\n" +
                            "the fundamental build steps (in order):\n" +
                            "---------------------------------------\n" +
                            "\n".join([s.id + s.help for s in bc.atomic_build_steps]) + "\n\n" +
                            "aliases / combinations of multiple of the above steps:\n" +
                            "------------------------------------------------------\n" +
                            "\n".join([s.id + s.help for s in bc.advanced_steps]),
                        metavar="build-steps",
                        nargs="*",
                        default=None,
                        # NOTE: an empty list is what is passed to "buildsteps" when the user does not specify any steps explicitly
                        choices=bc.avail_build_steps + [[]])

    #-----------------------------------------------------------------------
    # Build-Steps with Arguments
    #-----------------------------------------------------------------------
    for step_name in bc.atomic_build_step_sequence:
        parser.add_argument("--" + step_name,
                            help=argparse.SUPPRESS,
                            dest=step_name)

def get_parser():
    """Get a CLI parser instance that accepts all supported build.py parameters and commands"""
    parser = get_blank_parser()
    init_args(parser)
    return parser

def get_blank_parser():
    parser = argparse.ArgumentParser(prog="build", formatter_class=argparse.RawTextHelpFormatter)
    return parser
