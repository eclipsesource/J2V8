import argparse
import sys
import cross_build as cx

target_android = 'android'
target_linux = 'linux'
target_macos = 'macos'
target_win32 = 'win32'

arch_x86 = 'x86'
arch_x64 = 'x64'
arch_arm = 'arm'

# core build-steps
build_node_js = 'nodejs'
build_j2v8_jni = 'j2v8jni'
build_j2v8_java = 'j2v8java'

build_step_sequence = [
    build_node_js,
    build_j2v8_jni,
    build_j2v8_java,
]

targets = {
    target_android: {
        'architectures': [arch_x86, arch_arm],
        'cross_compiler': cx.DockerBuildSystem,
        'build_config':
            cx.BuildConfig("TODO", target_android,
            [
                cx.MountPoint("$HOST_CWD/node.out", "/build/node")
            ],
            "android-gcc-toolchain $ARCH --api 15 --host gcc-lpthread -C sh -c \"cd jni && ndk-build\""),
    },
    target_linux: {
        'architectures': [arch_x86, arch_x64],
        'cross_compiler': cx.DockerBuildSystem,
        'build_config':
            cx.BuildConfig("TODO" , "linux",
            [
                cx.MountPoint("$HOST_CWD/node.out", "/build/node"),
                cx.MountPoint("$HOST_CWD", "/build/."),
            ]),
    },
    target_macos: {
        'architectures': [arch_x86, arch_x64],
        'cross_compiler': cx.VagrantBuildSystem,
        'configs':
        {
            'cross': cx.BuildConfig(
                name = "cross-compile-host",
                platform = "macos",
                host_cwd = "$CWD/vagrant/$PLATFORM",
                build_cwd = "/Users/vagrant/j2v8",
            ),
            build_node_js: cx.BuildConfig(
                name = build_node_js,
                platform = "macos",
                build =
                [
                    "cd ./node",
                    "./configure --without-intl --without-inspector --dest-cpu=$ARCH --without-snapshot --enable-static",
                    "make -j4",
                ]
            ),
            build_j2v8_jni: cx.BuildConfig(
                name = build_j2v8_jni,
                platform = "macos",
                build =
                [
                    "mkdir -p cmake.out/$PLATFORM.$ARCH",
                    "cd cmake.out/$PLATFORM.$ARCH",
                    "rm -rf CMakeCache.txt",
                    "cmake ../../",
                    "make -j4",
                ]
            ),
            build_j2v8_java: cx.BuildConfig(
                name = build_j2v8_java,
                platform = "macos",
                build =
                [
                    #"export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_131.jdk/Contents/Home",
                    #"export PATH=/opt/apache-maven-3.5.0/bin:$PATH",
                    # "/Users/vagrant/./.profile",
                    "sudo mvn clean verify",
                ]
            ),
        }
    },
    target_win32: {
        'architectures': [arch_x86, arch_x64],
    },
}

parser = argparse.ArgumentParser()

parser.add_argument('--target', '-t',
                    dest='target',
                    choices=[
                        target_android,
                        target_linux,
                        target_macos,
                        target_win32,
                    ])

parser.add_argument('--arch', '-a',
                    dest='arch',
                    choices=[
                        arch_x86,
                        arch_x64,
                        arch_arm,
                    ])

parser.add_argument('--cross-compile', '-x',
                    dest='cross_compile',
                    action='store_const',
                    const=True)

parser.add_argument('--node-enabled', '-ne',
                    dest='node_enabled',
                    action='store_const',
                    const=True)

# aliases
build_all = 'all'
build_full = 'full'
build_native = 'native'
build_java = 'java'

build_options = [
    build_node_js,
    build_j2v8_jni,
    build_j2v8_java,
    build_all,
    build_full,
    build_native,
    build_java,
]

parser.add_argument('buildsteps',
                    metavar='build-step',
                    nargs='*',
                    default='all',
                    choices=build_options)

buildsteps = set()

def parse_build_step_option(step):
    return {
        build_all: add_all,
        build_full: add_all,
        build_native: add_native,
        build_java: add_managed,
        build_node_js: lambda: buildsteps.add(build_node_js),
        build_j2v8_jni: lambda: buildsteps.add(build_j2v8_jni),
        build_j2v8_java: lambda: buildsteps.add(build_j2v8_java),
    }.get(step, raise_unhandled_option)

def add_all():
    add_native()
    add_managed()

def add_native():
    buildsteps.add(build_node_js)
    buildsteps.add(build_j2v8_jni)

def add_managed():
    buildsteps.add(build_j2v8_java)

def raise_unhandled_option():
    sys.exit("INTERNAL-ERROR: Tried to handle unrecognized build-step")

args = parser.parse_args()

def execute_build(target, arch, steps, node_enabled = True, cross_compile = False):

    if (target is None):
        sys.exit("ERROR: No target platform specified, use --target <...>")

    if (not target in targets):
        sys.exit("ERROR: Unrecognized target platform: " + target)

    build_target = targets.get(target)

    if (arch is None):
        sys.exit("ERROR: No target architecture specified, use --arch <...>")

    build_architectures = build_target.get('architectures')

    if (not arch in build_architectures):
        sys.exit("ERROR: Unsupported architecture: \"" + arch + "\" for selected target platform: " + target)

    if (steps is None):
        sys.exit("ERROR: No build-step specified, valid values are: " + ", ".join(build_options))

    if (not steps is None and not isinstance(steps, list)):
        steps = [steps]

    global buildsteps
    buildsteps.clear()

    for step in steps:
        parse_build_step_option(step)()

    # force build-steps into defined order (see: http://stackoverflow.com/a/23529016)
    buildsteps = [step for step in build_step_sequence if step in buildsteps]

    configs = build_target.get('configs')

    if (cross_compile):
        x_compiler = build_target.get('cross_compiler')()
        x_config = configs.get('cross')
        x_cmd = "python ./build_platform.py -t $PLATFORM -a $ARCH " + ("-ne" if node_enabled else "") + " " + " ".join(buildsteps)
        x_compiler.build(x_config, arch, x_cmd)
    else:
        # TODO: get native build system Batch vs Shell
        host_compiler = cx.ShellBuildSystem()
        host_configs = dict(configs)
        del host_configs['cross']
        # build all requested build steps
        [host_compiler.build(host_configs[step], arch) for step in buildsteps]

if __name__ == '__main__':
    execute_build(args.target, args.arch, args.buildsteps, args.node_enabled, args.cross_compile)
