
import os

import build_system.build_utils as utils

branch = utils.get_node_branch_version()

print "Determined branch version name: " + branch

branch_patch_file = os.path.join("..", "node.patches", branch + ".diff")

print "Storing local changes to patch-file: " + branch_patch_file

utils.execute("git diff > " + branch_patch_file, "node")

print "Done"
