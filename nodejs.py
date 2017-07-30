import argparse
import collections
import glob
import os
import sys
import tarfile
import zipfile

import build_system.constants as c
import build_system.build_utils as utils
import build_system.build_settings as settings

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

    print platforms
    return

    # make sure all node.js binaries are stored in the cache before packaging
    flush_cache(True)

    # C++ header files
    included_paths = [
        DepsDirectory(path="./node/deps/", include=[".h"]),
        DepsDirectory(path="./node/src/", include=[".h"]),
    ]

    # Android
    if (full or c.target_android in platforms):
        included_paths += [
            DepsDirectory(path="./node.out/android.arm/", include=["j2v8.node.out", ".o", ".a"]),
            DepsDirectory(path="./node.out/android.x86/", include=["j2v8.node.out", ".o", ".a"]),
        ]

    # Linux
    if (full or c.target_linux in platforms):
        included_paths += [
            DepsDirectory(path="./node.out/linux.x64/", include=["j2v8.node.out", ".o", ".a"]),
            DepsDirectory(path="./node.out/linux.x86/", include=["j2v8.node.out", ".o", ".a"]),
        ]

    # MacOSX
    if (full or c.target_macos in platforms):
        included_paths += [
            DepsDirectory(path="./node.out/macos.x64/", include=["j2v8.node.out", ".a"]),
            DepsDirectory(path="./node.out/macos.x86/", include=["j2v8.node.out", ".a"]),
        ]

    # Windows
    if (full or c.target_win32 in platforms):
        included_paths += [
            DepsDirectory(path="./node.out/win32.x64/", include=["j2v8.node.out", ".lib"]),
            DepsDirectory(path="./node.out/win32.x86/", include=["j2v8.node.out", ".lib"]),
        ]

    with tarfile.open("j2v8-nodejs-deps-" + settings.J2V8_VERSION + ".tar.bz2", "w:bz2") as zipf:
    # with zipfile.ZipFile("j2v8-nodejs-deps-" + settings.J2V8_VERSION + ".zip", "w", zipfile.ZIP_DEFLATED) as zipf:
        for curr_p in included_paths:
            print "zipping " + curr_p.path
            dir_path = os.path.normpath(curr_p.path)

            for root, dirs, files in os.walk(dir_path):
                for f in files:
                    file_path = os.path.join(root, f)

                    copy_file = False

                    for pattern in curr_p.include:
                        if (file_path.endswith(pattern)):
                            copy_file = True
                            break

                    if (copy_file):
                        if (os.stat(file_path).st_size > 1024 * 1024):
                            print file_path

                        # zipf.write(file_path)
                        zipf.add(file_path)

    print "Done"

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

args = parser.parse_args()

for cmd_tuple in all_cmds:
    if (args.cmd[0] in cmd_tuple.aliases):
        cmd_tuple.function()
        break
