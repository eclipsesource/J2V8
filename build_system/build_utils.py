import collections
import glob
import os
import re
import shutil
import subprocess
import sys
# from itertools import ifilter

from . import constants as c

V8Version = collections.namedtuple("V8Version", "major minor build patch is_candidate")
NodeJSVersion = collections.namedtuple("NodeJSVersion", "major minor patch is_release")

def get_cwd():
    return os.getcwd().replace("\\", "/")

def host_cmd_sep():
    return "&& " if os.name == "nt" else "; "

def touch(filename, times=None):
    with open(filename, 'a'):
        os.utime(filename, times)

def is_android(platform):
    return c.target_android in platform

def is_linux(platform):
    return c.target_linux in platform

def is_macos(platform):
    return c.target_macos in platform

def is_win32(platform):
    return c.target_win32 in platform

def platform_libext(config):
    lib_ext = "so"

    if (is_win32(config.platform)):
        lib_ext = "dll"

    elif (is_macos(config.platform)):
        lib_ext = "dylib"

    return lib_ext

def cli_exit(message):
    """
    sys.exit() messages are not picked up correctly when unit-testing.
    Use this function instead!
    """
    sys.stderr.write(message + "\n")
    sys.stderr.flush()
    sys.exit(1)

def get_v8_version():
    v8_version_text = None

    with file("./v8.out/include/v8-version.h", "r") as v8_version_file:
        v8_version_text = v8_version_file.read()

        major = re.search(r"#define V8_MAJOR_VERSION (\d+)", v8_version_text)
        minor = re.search(r"#define V8_MINOR_VERSION (\d+)", v8_version_text)
        build = re.search(r"#define V8_BUILD_NUMBER (\d+)", v8_version_text)
        patch = re.search(r"#define V8_PATCH_LEVEL (\d+)", v8_version_text)

        is_candidate = re.search(r"#define V8_IS_CANDIDATE_VERSION (\d+)", v8_version_text)

        major = major.group(1) if major else None
        minor = minor.group(1) if minor else None
        build = build.group(1) if build else None
        patch = patch.group(1) if patch else None

        is_candidate = is_candidate.group(1) == "1" if is_candidate else None

        return V8Version(major, minor, build, patch, is_candidate)

def get_nodejs_version():
    njs_version_text = None

    with file("./node/src/node_version.h", "r") as njs_version_file:
        njs_version_text = njs_version_file.read()

        major = re.search(r"#define NODE_MAJOR_VERSION (\d+)", njs_version_text)
        minor = re.search(r"#define NODE_MINOR_VERSION (\d+)", njs_version_text)
        patch = re.search(r"#define NODE_PATCH_VERSION (\d+)", njs_version_text)

        is_release = re.search(r"#define NODE_VERSION_IS_RELEASE (\d+)", njs_version_text)

        major = major.group(1) if major else None
        minor = minor.group(1) if minor else None
        patch = patch.group(1) if patch else None

        is_release = is_release.group(1) == "1" if is_release else None

        return NodeJSVersion(major, minor, patch, is_release)

# based on code from: https://stackoverflow.com/a/16260159/425532
def readlines(f, newlines):
    buf = ""
    while True:
    #{
        def get_pos():
        #{
            pos = None
            nl = None
            for n in newlines:
                if pos:
                    break
                try:
                    pos = buf.index(n)
                except Exception:
                    pass

                if pos:
                    nl = n

            return (pos, nl)
        #}

        pos, nl = get_pos()

        while pos:
            yield buf[:pos] + nl
            buf = buf[pos + len(nl):]
            pos, nl = get_pos()

        chunk = f.read(1)

        if chunk == ":":
            # read another char to make sure we catch ": " delimiter
            buf += chunk
            chunk = f.read(1)

        if not chunk:
            yield buf
            break
        buf += chunk
    #}

redirect_stdout_enabled = False

def execute(cmd, cwd = None):
    """
    Low-Level CLI utility function to execute a shell command in a sub-process of the current python process
    (redirects all output to the host-process stdout if redirect_stdout_enabled is True)
    """
    if not redirect_stdout_enabled:
        # flush any buffered console output, because popen could block the terminal
        sys.stdout.flush()

        p = subprocess.Popen(cmd, universal_newlines=True, shell=True, cwd=cwd)
        return_code = p.wait()

        if return_code:
            raise subprocess.CalledProcessError(return_code, cmd)
    else:
        # see: https://stackoverflow.com/a/22049757/425532
        # this way of running the process and handling the process output is important because
        # when running unit-tests in python or running e.g. a docker process, if the
        # output does not directly go through the stdout of the python process,
        # then it will not be picked up by some of the available unit-test runners

        # flush any buffered console output, because popen could block the terminal
        sys.stdout.flush()

        p = subprocess.Popen(cmd,
            shell=True,
            cwd=cwd,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT#,
            #stdin=sys.stdin
        )
        # while True:
        #     line = p.stdout.readline()
        #     if line == '':
        #         break
        #     print(line.strip("\r\n"))  # remove extra ws between lines
        #     sys.stdout.flush()

        # also look for ": " as a output separator, because Vagrant emits this
        # right before some relevant user input is requested
        # (this makes sure that we get all output for the input is required)
        for line in readlines(p.stdout, [": ", ":", "\n"]):
            sys.stdout.write(line)
            sys.stdout.flush()

        return_code = p.wait()

        if return_code:
            raise subprocess.CalledProcessError(return_code, cmd)

def execute_to_str(cmd, cwd = None):
    """
    Low-Level CLI utility function to execute a shell command in a sub-process of the current python process
    (returns all output as a string)
    """
    # flush any buffered console output, because popen could block the terminal
    sys.stdout.flush()

    p = subprocess.Popen(cmd, stdout=subprocess.PIPE, universal_newlines=True, shell=True, cwd=cwd)
    out, err = p.communicate()

    if (not err is None):
        raise subprocess.CalledProcessError(p.returncode, cmd, err)

    return out

def store_v8_output(image_name, config):
    print ("Storing V8 output from '{0}' docker image".format(image_name))
    build_cwd = get_cwd()
    dest_cpu= c.arch_x64 if config.arch == c.arch_x86_64 else config.arch
    static_library_output_dir = config.platform + "." + dest_cpu
    dest_cpu= 'ia32' if config.arch == c.arch_x86 else dest_cpu
    execute("""docker run --rm -v {0}/v8.out:/mount {1} /bin/bash -c "cp -R include /mount && mkdir -p /mount/{3}/ && cp -R out.gn/{2}.release/obj/libv8_monolith.a /mount/{3}/" """.format(build_cwd, image_name, dest_cpu, static_library_output_dir))

def store_nodejs_output(next_node_tag, build_cwd):
    """Cache built Node.js artifacts into a common directory structure, identified by vendor, platform and architecture."""
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
            with open(curr_tag_file, "r") as f:
                curr_node_tag = f.read()

    if (curr_node_tag != next_node_tag):
        if (curr_node_tag is not None):
            print (">>> Storing Node.js build files for later use: " + curr_node_tag)

            for subdir in ["out"] + extra_dirs:
                curr_cache = cached_dir(curr_node_tag, subdir)
                node = curr_dir(subdir)

                # we want to store into the cache, delete any existing directories that might
                # already occupy the cache subdir (there should be none)
                if (os.path.isdir(curr_cache)):
                    shutil.rmtree(curr_cache)

                # move the previously built artifacts into the cache
                if (os.path.isdir(node)):
                    print ("node --- " + subdir + " ---> cache[" + curr_node_tag + "]")
                    shutil.move(node, curr_cache)

        if (next_node_tag is None):
            return

        next_dir = cached_dir(next_node_tag, "out")

        if (os.path.isdir(next_dir)):
            print (">>> Reused Node.js build files from build-cache: " + next_node_tag)
            print ("node <--- out --- cache[" + next_node_tag + "]")
            # move main node.js "out" directory from the cache back into the node directory
            shutil.move(next_dir, out_dir)

            # also move any extra dirs from the cache back into node
            for subdir in extra_dirs:
                node = curr_dir(subdir)
                next_cache = cached_dir(next_node_tag, subdir)

                if (os.path.isdir(next_cache)):
                    print ("node <--- " + subdir + " --- cache[" + next_node_tag + "]")
                    shutil.move(next_cache, node)
        else:
            print (">>> Prepared Node.js build-cache: " + next_node_tag)

            # create fresh out-dir in the cache to receive build artifacts ...
            if not os.path.exists(out_dir):
                os.makedirs(out_dir)

            # ... and immediately also create a tag-file so we know what we built later on
            with open(curr_tag_file, "w") as f:
                f.write(next_node_tag)

    elif (not next_node_tag is None):
        # this build is for the same vendor/platform/architecture as last time
        print (">>> Node.js build-cache used: " + next_node_tag)

def apply_file_template(src, dest, inject_vars_fn):
    """Read a text file from src, run the read text through a transformer function and write the modified text into dest"""
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
    """
    The function compares the list of builtin Node.js modules with the setup
    code in jni/com_eclipsesource_v8_V8Impl.cpp to make sure that every module
    is correctly initialized and linked into the native J2V8 library.
    """
    node_src = "./node/src/"

    # node.js directory is not available
    if (not os.path.exists(node_src)):
        return

    # building from a pre-built dependency package (does not include c++ source files)
    if (len(glob.glob(node_src + "*.cc")) == 0):
        return

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

        cli_exit(error)
