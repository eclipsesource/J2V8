import os
import re
import shutil
import subprocess
import sys
from itertools import ifilter

import constants

def get_cwd():
    return os.getcwd().replace("\\", "/")

def get_node_branch_version():
    out = execute_to_str("git branch", "node")

    git_branch_lines = out.splitlines()

    branch_str = next(ifilter(lambda x: x.startswith("*"), git_branch_lines), None)

    print "Git active branch: " + branch_str

    branch_match = re.search(r"\* \(HEAD detached at v(.*)\)", branch_str)

    if (branch_match is None):
        branch_match = re.search(r"\* \((.*)\)", branch_str)

    if (branch_match is None):
        sys.exit("ERROR: Unrecognized branch name format while running 'git branch': " + branch_str)

    branch = branch_match.group(1)
    return branch

def execute(cmd, cwd = None):
    popen = subprocess.Popen(cmd, universal_newlines=True, shell=True, cwd=cwd)
    return_code = popen.wait()
    if return_code:
        raise subprocess.CalledProcessError(return_code, cmd)

def execute_to_str(cmd, cwd = None):
    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, cwd=cwd)
    out, err = p.communicate()

    if (not err is None):
        raise subprocess.CalledProcessError(p.returncode, cmd, err)

    return out

def store_nodejs_output(next_node_tag, build_cwd):
    curr_node_tag = None

    curr_dir = lambda subdir: build_cwd + "/node/" + subdir
    cached_dir = lambda tag, subdir: build_cwd + "/node.out/" + tag + "/" + subdir

    out_dir = curr_dir("out")
    extra_dirs = [
        "build",
        "Release",
        "Debug",
    ]

    curr_tag_file = out_dir + "/j2v8.node.out"

    if (os.path.isdir(out_dir)):
        if (os.path.exists(curr_tag_file)):
            with open(curr_tag_file, 'r') as f:
                curr_node_tag = f.read()

    if (curr_node_tag != next_node_tag):
        if (curr_node_tag is not None):
            print ">>> Storing Node.js build files for later use: " + curr_node_tag

            for subdir in ["out"] + extra_dirs:
                curr_cache = cached_dir(curr_node_tag, subdir)
                node = curr_dir(subdir)

                # we want to store into the cache, delete any existing directories that might
                # already occupy the cache (there should not be one)
                if (os.path.isdir(curr_cache)):
                    shutil.rmtree(curr_cache)

                # move the previous build artifacts into the cache
                if (os.path.isdir(node)):
                    print "node --- " + subdir + " ---> cache[" + curr_node_tag + "]"
                    shutil.move(node, curr_cache)

        if (next_node_tag is None):
            return

        next_dir = cached_dir(next_node_tag, "out")

        if (os.path.isdir(next_dir)):
            print ">>> Reused Node.js build files from previous build: " + next_node_tag
            print "node <--- out --- cache[" + next_node_tag + "]"
            shutil.move(next_dir, out_dir)

            # move extra dirs from cache into node
            for subdir in extra_dirs:
                node = curr_dir(subdir)
                next_cache = cached_dir(next_node_tag, subdir)

                if (os.path.isdir(next_cache)):
                    print "node <--- " + subdir + " --- cache[" + next_node_tag + "]"
                    shutil.move(next_cache, node)
        else:
            print ">>> Prepared Node.js output for caching: " + next_node_tag

            # create fresh out-dir to receive build artifacts ...
            if not os.path.exists(out_dir):
                os.makedirs(out_dir)
            # ... and immediately also create a tag-file so we know what we built later on
            with open(curr_tag_file, 'w') as f:
                f.write(next_node_tag)

    elif (not next_node_tag is None):
        print ">>> Used existing Node.js build files: " + next_node_tag

def apply_file_template(src, dest, inject_vars_fn):
    template_text = None
    with open(src, "r") as f:
        template_text = f.read()

    template_text = inject_vars_fn(template_text)

    with open(dest, "w") as f:
        f.write(template_text)
