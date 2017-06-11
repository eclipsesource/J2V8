import os
import shutil

def get_cwd():
    return os.getcwd().replace("\\", "/")

def store_nodejs_output(platform, arch, build_cwd):
    prev_node_tag = None
    curr_node_tag = platform + "." + arch

    out_dir = build_cwd + "/node/out"
    tagged_dir = lambda tag: build_cwd + "/node.out/" + tag + "/out"

    if (os.path.isdir(out_dir)):
        curr_f = out_dir + "/j2v8.node.out"
        if (os.path.exists(curr_f)):
            with open(curr_f, 'r') as f:
                prev_node_tag = f.read()

    if (prev_node_tag != curr_node_tag):
        if (prev_node_tag is not None):
            print ">>> Stored Node.js build files for later use: " + prev_node_tag
            prev_dir = tagged_dir(prev_node_tag)

            if (os.path.isdir(prev_dir)):
                shutil.rmtree(prev_dir)

            shutil.move(out_dir, prev_dir)

        curr_dir = tagged_dir(curr_node_tag)

        if (os.path.isdir(curr_dir)):
            print ">>> Reused Node.js build files from previous build: " + curr_node_tag
            shutil.move(curr_dir, out_dir)
        else:
            if not os.path.exists(out_dir):
                os.makedirs(out_dir)
            with open(curr_f, 'w') as f:
                f.write(curr_node_tag)
    else:
        print ">>> Used existing Node.js build files: " + curr_node_tag

def applyFileTemplate(src, dest, inject_vars_fn):
    template_text = None
    with open(src, "r") as f:
        template_text = f.read()

    template_text = inject_vars_fn(template_text)

    with open(dest, "w") as f:
        f.write(template_text)
