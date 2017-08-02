import argparse

import constants as c
import build_constants as bc

class BuildParams(object):
    """Value container for all build-parameters"""
    def __init__(self, d):
        self.target = d.get("target")
        self.arch = d.get("arch")
        self.vendor = d.get("vendor")
        self.keep_native_libs = d.get("keep_native_libs")
        self.node_enabled = d.get("node_enabled")
        self.docker = d.get("docker")
        self.vagrant = d.get("vagrant")
        self.sys_image = d.get("sys_image")
        self.no_shutdown = d.get("no_shutdown")
        self.buildsteps = d.get("buildsteps") or c.build_all

        self.cross_agent = None

def init_args(parser):
    """Initialize all supported build.py parameters and commands on the CLI parser"""

    # Essential build settings
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

    # Optional build settings
    parser.add_argument("--vendor", "-v",
                        help="The operating system vendor (most relevant when building for a specific Linux distribution).",
                        dest="vendor")

    parser.add_argument("--keep-native-libs", "-knl",
                        help="Do not delete the native J2V8 libraries from the Java directories between builds.",
                        dest="keep_native_libs",
                        default=False,
                        action="store_const",
                        const=True)

    # J2V8 Feature switches
    parser.add_argument("--node-enabled", "-ne",
                        help="Include the Node.js runtime and builtin node-modules for use in J2V8.",
                        dest="node_enabled",
                        default=False,
                        action="store_const",
                        const=True)

    # Docker / Vagrant cross-compile settings
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

    # Meta-Args
    # NOTE: this option is only used internally to distinguish the running of the build script within
    # the build-instigator and the actual build-executor (this is relevant when cross-compiling)
    parser.add_argument("--cross-agent",
                        help=argparse.SUPPRESS,
                        dest="cross_agent",
                        type=str)

    parser.add_argument("--interactive", "-i",
                    help="Run the interactive version of the J2V8 build CLI.",
                    dest="interactive",
                    default=False,
                    action="store_const",
                    const=True)

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
                        default="all",
                        choices=bc.avail_build_steps)

def get_parser():
    """Get a CLI parser instance that accepts all supported build.py parameters and commands"""
    parser = argparse.ArgumentParser(formatter_class=argparse.RawTextHelpFormatter)
    init_args(parser)
    return parser
