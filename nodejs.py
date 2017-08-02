"""
Utility-belt script to manage the Node.js dependency
"""
import argparse
import collections
import fnmatch
import glob
import io
import os
import sys
import tarfile
import zipfile

import build_system.constants as c
import build_system.build_constants as bc
import build_system.build_utils as utils
import build_system.build_settings as settings

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

Command = collections.namedtuple("Command", "aliases function")
DepsDirectory = collections.namedtuple("DepsDirectory", "path include")

# Command-Line setup
parser = argparse.ArgumentParser(formatter_class=argparse.RawTextHelpFormatter)

#-----------------------------------------------------------------------
def flush_cache(silent = False):
    if not silent:
        print "[flush-cache]"

    utils.store_nodejs_output(None, ".")

    if not silent:
        print "Done" 

cmd_flush_cache = Command(
    aliases=["flush-cache", "fc"],
    function=flush_cache,
)
#-----------------------------------------------------------------------
def git_init():
    print "[git-init]"

    # TODO: add CLI overide options
    # - Node version
    # - J2V8 version

    utils.store_nodejs_output(None, ".")

    if (not os.path.exists("node")):
        print "Cloning Node.js version: " + settings.NODE_VERSION
        # NOTE: autocrlf=false is very important for linux based cross-compiles of Node.js to work on a windows docker host
        utils.execute("git clone https://github.com/nodejs/node --config core.autocrlf=false --depth 1 --branch v" + settings.NODE_VERSION)
    else:
        print "Node.js is already cloned & checked out"
        apply_diff(True)

    print "Done"

cmd_git_init = Command(
    aliases=["git-init", "gi"],
    function=git_init
)
#-----------------------------------------------------------------------
def package():
    print "[package]"

    platforms = sys.argv[2:]
    full = len(platforms) == 0

    # make sure all node.js binaries are stored in the cache before packaging
    flush_cache(True)

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

    # speciffy the platforms & file patterns that should be included
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
    aliases=["package", "pkg"],
    function=package
)
#-----------------------------------------------------------------------
def store_diff():
    print "[store-diff]"

    patch_file = os.path.join("..", "node.patches", settings.NODE_VERSION + ".diff")
    print "Storing local changes to patch-file: " + patch_file

    utils.execute("git diff > " + patch_file, "node")
    print "Done"

cmd_store_diff = Command(
    aliases=["store-diff", "sd"],
    function=store_diff
)
#-----------------------------------------------------------------------
def apply_diff(silent = False):
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
    aliases=["apply-diff", "ad"],
    function=apply_diff
)
#-----------------------------------------------------------------------

all_cmds = [
    cmd_flush_cache,
    cmd_git_init,
    cmd_package,
    cmd_store_diff,
    cmd_apply_diff,
]

parser.add_argument("cmd",
                    metavar="command",
                    nargs=1,
                    type=str,
                    choices=[cmd for commands in all_cmds for cmd in commands.aliases])

parser.add_argument("rest",
                    nargs="*",
                    help=argparse.SUPPRESS)

args = parser.parse_args()

for cmd_tuple in all_cmds:
    if (args.cmd[0] in cmd_tuple.aliases):
        cmd_tuple.function()
        break
