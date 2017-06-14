
import os
import sys

import build_system.build_utils as utils
import build_settings as settings

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
    branch = utils.get_node_branch_version()

    if (branch != settings.NODE_VERSION):
        sys.exit("ERROR: The checked out Node.js version (" + branch + ") does not match the version specified in build_settings.py (" + settings.NODE_VERSION + ")")

branch_patch_file = os.path.join("node.patches", settings.NODE_VERSION + ".diff")

if (os.path.exists(branch_patch_file)):
    print "Applying Node.js patch: " + branch_patch_file
    utils.execute("git apply " + os.path.join("..", branch_patch_file), "node")
else:
    print "No special Node.js patch present for this version"

print "Done"
