import collections
import os
import re
import shutil
import subprocess
import sys
from itertools import ifilter

import constants as c

import constants

def get_cwd():
    return os.getcwd().replace("\\", "/")

def host_cmd_sep():
    return "&& " if os.name == "nt" else "; "

def is_android(platform):
    return c.target_android in platform

def is_linux(platform):
    return c.target_linux in platform

def is_macos(platform):
    return c.target_macos in platform

def is_win32(platform):
    return c.target_win32 in platform

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
    # flush any buffered console output, because popen could block the terminal
    sys.stdout.flush()

    p = subprocess.Popen(cmd, universal_newlines=True, shell=True, cwd=cwd)
    return_code = p.wait()
    if return_code:
        raise subprocess.CalledProcessError(return_code, cmd)

def execute_to_str(cmd, cwd = None):
    # flush any buffered console output, because popen could block the terminal
    sys.stdout.flush()

    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, universal_newlines=True, shell=True, cwd=cwd)
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

#-----------------------------------------------------------------------
# Sanity check for the builtin node-module links in J2V8 C++ JNI code
#-----------------------------------------------------------------------
def check_node_builtins():
    j2v8_jni_cpp_path = "jni/com_eclipsesource_v8_V8Impl.cpp"
    j2v8_builtins = []

    with open(j2v8_jni_cpp_path, "r") as j2v8_jni_cpp:
        j2v8_code = j2v8_jni_cpp.read()

    tag = "// @node-builtins-force-link"
    start = j2v8_code.find(tag)

    end1 = j2v8_code.find("}", start)
    end2 = j2v8_code.find("#endif", start)

    if (end1 < 0 and end2 < 0):
        return

    end = min(int(e) for e in [end1, end2])

    if (end < 0):
        return

    j2v8_linked_builtins = j2v8_code[start + len(tag):end]

    j2v8_builtins = [m for m in re.finditer(r"^\s*_register_(?P<name>.+)\(\);\s*$", j2v8_linked_builtins, re.M)]

    comment_tokens = ["//", "/*", "*/"]

    j2v8_builtins = [x.group("name") for x in j2v8_builtins if not any(c in x.group(0) for c in comment_tokens)]

    node_src = "node/src/"
    node_builtins = []
    for cc_file in os.listdir(node_src):
        if (not cc_file.endswith(".cc")):
            continue

        with open(node_src + cc_file, "r") as node_cpp:
            node_code = node_cpp.read()

        m = re.search(r"NODE_MODULE_CONTEXT_AWARE_BUILTIN\((.*),\s*node::.*\)", node_code)

        if (m is not None):
            node_builtins.append(m.group(1))

    # are all Node.js builtins mentioned?
    builtins_ok = collections.Counter(j2v8_builtins) == collections.Counter(node_builtins)

    if (not builtins_ok):
        j2v8_extra = [item for item in j2v8_builtins if item not in node_builtins]
        j2v8_missing = [item for item in node_builtins if item not in j2v8_builtins]

        error = "ERROR: J2V8 linking builtins code does not match Node.js builtin modules, check " + j2v8_jni_cpp_path

        if (len(j2v8_extra) > 0):
            error += "\n\t" + "J2V8 defines unrecognized node-modules: " + str(j2v8_extra)

        if (len(j2v8_missing) > 0):
            error += "\n\t" + "J2V8 definition is missing node-modules: " + str(j2v8_missing)

        sys.exit(error)