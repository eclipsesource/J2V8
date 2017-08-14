"""
Utility-belt script to manage the Node.js/V8 dependency
"""
import argparse
import collections
import fnmatch
import glob
import io
from itertools import ifilter
import os
import sys
import tarfile
import zipfile

import build_system.constants as c
import build_system.build_constants as bc
import build_system.build_utils as utils
import build_system.build_settings as settings

CMD_LINEBREAK = "\n\n"

# helper classes to show zipping progress
# original idea: https://stackoverflow.com/a/3668977/425532
class ReadProgressFileObject(io.FileIO):
    current_read = 0
    def __init__(self, path, *args, **kwargs):
        io.FileIO.__init__(self, path, *args, **kwargs)

    def read(self, size):
        b = io.FileIO.read(self, size)
        ReadProgressFileObject.current_read += len(b)
        return b

class WriteProgressFileObject(io.FileIO):
    def __init__(self, path, size, *args, **kwargs):
        self._total_size = size
        io.FileIO.__init__(self, path, *args, **kwargs)

    def write(self, b):
        progress = min(100.0, ReadProgressFileObject.current_read / (self._total_size * 0.01))
        sys.stdout.write("\r[%3.2f%%] " %(progress))
        sys.stdout.flush()
        return io.FileIO.write(self, b)

Command = collections.namedtuple("Command", "name function help")
DepsDirectory = collections.namedtuple("DepsDirectory", "path include")

#-----------------------------------------------------------------------
def flush_cache(args = None, silent = False):
    if not silent:
        print "[flush-cache]"

    utils.store_nodejs_output(None, ".")

    if not silent:
        print "Done" 

cmd_flush_cache = Command(
    name="flush-cache",
    function=flush_cache,
    help="Move any Node.js/V8 native build-artifacts (.o/.a/.lib) from the './node' directory into the 'node.out' cache subdirectory\n" + \
    "         of the respective vendor/platform/architecture."
)
#-----------------------------------------------------------------------
def git_clone(args):
    print "[git-clone]"

    # TODO: add CLI overide options
    # - Node version
    # - J2V8 version

    flush_cache(silent=True)

    if (not os.path.exists("node")):
        print "Cloning Node.js version: " + settings.NODE_VERSION
        # NOTE: autocrlf=false is very important for linux based cross-compiles of Node.js to work on a windows docker host
        utils.execute("git clone https://github.com/nodejs/node --config core.autocrlf=false --depth 1 --branch v" + settings.NODE_VERSION)
    else:
        print "Skipped git-clone: Node.js source-code is already cloned & checked out at the './node' directory."

    print "Done"

cmd_git_clone = Command(
    name="git-clone",
    function=git_clone,
    help="   Clone the C++ source-code from the official Node.js GitHub repository." + \
    "\n            (the Node.js version branch from build_settings.py will be checked out automatically)"
)
#-----------------------------------------------------------------------
def git_checkout(args):
    print "[git-checkout]"

    flush_cache(silent=True)

    if (os.path.exists("node")):
        print "Checkout Node.js version: " + settings.NODE_VERSION

        # TODO: is there a way to fetch/checkout only a single remote tag
        utils.execute("git fetch -v --progress --tags --depth 1 origin", "node")
        utils.execute("git checkout --progress tags/v" + settings.NODE_VERSION + " -b v" + settings.NODE_VERSION, "node")
    else:
        print "ERROR: Node.js source-code was not yet cloned into the './node' directory, run 'python nodejs.py git-clone' first."

    print "Done"

cmd_git_checkout = Command(
    name="git-checkout",
    function=git_checkout,
    help="Checkout the correct git branch for the Node.js version specified in build_settings.py"
)
#-----------------------------------------------------------------------
def package(platforms = None):
    print "[package]"

    full = platforms == None or len(platforms) == 0

    # make sure all node.js binaries are stored in the cache before packaging
    flush_cache(silent=True)

    # C++ header files
    # NOTE: see https://stackoverflow.com/a/4851555/425532 why this weird syntax is necessary here
    dependencies = {
        "list": [
            DepsDirectory(path="./node/deps/", include=[".h"]),
            DepsDirectory(path="./node/src/", include=[".h"]),
        ],
        "size": 0,
    }

    def __add_platform_deps(platform, include, vendor = None):
        target = bc.platform_configs.get(platform)
        vendor_str = (vendor + "-" if vendor else "")
        selected = (vendor_str + platform) in platforms

        if (full or selected):
            dependencies["list"] += [
                DepsDirectory(
                    path="./node.out/" + vendor_str + platform + "." + arch + "/",
                    include=["j2v8.node.out"] + include
                ) for arch in target.architectures
            ]

    # specify the platforms & file patterns that should be included
    __add_platform_deps(c.target_android, [".o", ".a"])
    __add_platform_deps(c.target_linux, [".o", ".a"])
    __add_platform_deps(c.target_linux, [".o", ".a"], vendor = c.vendor_alpine)
    __add_platform_deps(c.target_macos, [".a"])
    __add_platform_deps(c.target_win32, [".lib"])

    # could be a package for an individual platform, or a complete package
    package_platform = platforms[0] + "-" if len(platforms) == 1 else ""
    package_filename = "j2v8-nodejs-deps-" + package_platform + settings.J2V8_VERSION + ".tar.bz2"

    # determine the uncompressed total size of all included files
    for dep in dependencies["list"]:
        print "scan " + dep.path
        for root, dirs, filenames in os.walk(dep.path):
            for pattern in dep.include:
                for file_name in fnmatch.filter(filenames, '*' + pattern):
                    file_path = os.path.join(root, file_name)
                    dependencies["size"] += os.path.getsize(file_path)

    # start zipping the package
    with tarfile.open(fileobj=WriteProgressFileObject(package_filename, dependencies["size"], "w"), mode="w:bz2") as zipf:
    # with tarfile.open(package_filename, "w:bz2") as zipf:
    # with zipfile.ZipFile("j2v8-nodejs-deps-" + settings.J2V8_VERSION + ".zip", "w", zipfile.ZIP_DEFLATED) as zipf:
        for dep in dependencies["list"]:
            print "compress " + dep.path
            dir_path = os.path.normpath(dep.path)

            for root, dirs, files in os.walk(dir_path):
                for f in files:
                    file_path = os.path.join(root, f)

                    copy_file = False

                    for pattern in dep.include:
                        if (file_path.endswith(pattern)):
                            copy_file = True
                            break

                    if (copy_file):
                        # only show files > 1 MB
                        if (os.path.getsize(file_path) > 1024 * 1024):
                            print file_path

                        # zipf.write(file_path)
                        # zipf.add(file_path)
                        info = zipf.gettarinfo(file_path)
                        zipf.addfile(info, ReadProgressFileObject(file_path))

    print "Done"
    print "generated: " + package_filename

cmd_package = Command(
    name="package",
    function=package,
    help="Create a .tar.bz2 dependency package with all the currently built Node.js/V8 binaries from the './node.out' cache directories."
)
#-----------------------------------------------------------------------
def touch(platforms = None):
    full = platforms == None or len(platforms) == 0

    # make sure all node.js binaries are stored in the cache before resetting file-times
    flush_cache(silent=True)

    dependencies = {
        "list": [],
    }

    # TODO: extract shared code between this and "package" command
    def __add_platform_deps(platform, include, vendor = None):
        target = bc.platform_configs.get(platform)
        vendor_str = (vendor + "-" if vendor else "")
        selected = (vendor_str + platform) in platforms

        if (full or selected):
            dependencies["list"] += [
                DepsDirectory(
                    path="./node.out/" + vendor_str + platform + "." + arch + "/",
                    include=["j2v8.node.out"] + include
                ) for arch in target.architectures
            ]

    # specify the platforms & file patterns that should be included
    __add_platform_deps(c.target_android, [".o", ".a"])
    __add_platform_deps(c.target_linux, [".o", ".a"])
    __add_platform_deps(c.target_linux, [".o", ".a"], vendor = c.vendor_alpine)
    __add_platform_deps(c.target_macos, [".a"])
    __add_platform_deps(c.target_win32, [".lib"])

    # set modification-time of all found binary files
    for dep in dependencies["list"]:
        print "set current file-time " + dep.path
        for root, dirs, filenames in os.walk(dep.path):
            for pattern in dep.include:
                for file_name in fnmatch.filter(filenames, '*' + pattern):
                    file_path = os.path.join(root, file_name)
                    utils.touch(file_path)

cmd_touch = Command(
    name="touch",
    function=touch,
    help="Set modification-time of all currently built Node.js/V8 binaries in the './node.out' cache directories."
)
#-----------------------------------------------------------------------
def store_diff(args):
    print "[store-diff]"

    patch_file = os.path.join("..", "node.patches", settings.NODE_VERSION + ".diff")
    print "Storing local changes to patch-file: " + patch_file

    utils.execute("git diff > " + patch_file, "node")
    print "Done"

cmd_store_diff = Command(
    name="store-diff",
    function=store_diff,
    help="Create a patch-file in the './node.patches' directory with the current local modifications\n" +
    "          to the Node.js/V8 source-code.\n" +
    "          (the Node.js version from build_settings.py will be included in the patch filename)."
)
#-----------------------------------------------------------------------
def apply_diff(args, silent = False):
    if not silent:
        print "[apply-diff]"

    patch_file = os.path.join("node.patches", settings.NODE_VERSION + ".diff")

    if (os.path.exists(patch_file)):
        print "Applying Node.js patch: " + patch_file
        utils.execute("git apply " + os.path.join("..", patch_file), "node")
    else:
        print "No special Node.js patch present for this version"

    if not silent:
        print "Done"

cmd_apply_diff = Command(
    name="apply-diff",
    function=apply_diff,
    help=" Apply a previously created patch-file to the currently checked out Node.js/V8 source-code."
)
#-----------------------------------------------------------------------

#-----------------------------------------------------------------------
# Command-Line setup
#-----------------------------------------------------------------------
commands = {
    "git": {
        "__help": " Download and manage the Node.js/V8 source code for building J2V8 from source.",
        "clone": cmd_git_clone,
        "checkout": cmd_git_checkout,
    },
    "bin": {
        "__help": " Manage the binary build-artifacts that are produced by Node.js/V8 builds.",
        "flush": cmd_flush_cache,
        "package": cmd_package,
        "touch": cmd_touch,
    },
    "diff": {
        "__help": "Create and apply Git patch-files for Node.js that are required for interoperability with J2V8.",
        "create": cmd_store_diff,
        "apply": cmd_apply_diff,
    },
}
#-----------------------------------------------------------------------
def parse_sub_command(args, choices, help_formatter, extra_args = None):
    parser = argparse.ArgumentParser(formatter_class=argparse.RawTextHelpFormatter)
    help_str = [c + "    " + help_formatter(c) for c in choices]
    parser.add_argument("command", help="\n\n".join(help_str) + "\n\n", choices=choices)

    if (extra_args):
        extra_args(parser)

    args = parser.parse_args(args)
    return args
#-----------------------------------------------------------------------

# parse first level command
args = parse_sub_command(sys.argv[1:2], commands, lambda c: commands[c].get("__help"))
lvl1_cmd = commands.get(args.command)

# parse second level command
sub_choices = filter(lambda x: x != "__help", lvl1_cmd)
args = parse_sub_command(sys.argv[2:], sub_choices, lambda c: lvl1_cmd[c].help, \
    lambda parser: parser.add_argument("args", nargs="*"))
lvl2_cmd = args.command

# get the final command handler and delegate all further parameters to it
cmd_handler = lvl1_cmd.get(lvl2_cmd)
cmd_handler.function(sys.argv[3:])
