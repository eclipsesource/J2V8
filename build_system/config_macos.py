import os
from . import constants as c
from .build_structures import PlatformConfig
from .vagrant_build import VagrantBuildSystem, VagrantBuildStep
from . import java_build_steps as j
from . import shared_build_steps as u
from . import cmake_utils as cmu

macos_config = PlatformConfig(c.target_macos, [c.arch_x86, c.arch_x64, c.arch_arm64])

macos_config.set_cross_configs({
    "vagrant": VagrantBuildStep(
        platform=c.target_macos,
        host_cwd="$CWD/vagrant/$PLATFORM",
        build_cwd="/Users/vagrant/j2v8",
        pre_build_cmd = u.setEnvVar("VAGRANT_FILE_SHARE_TYPE", "smb" if os.name == "nt" else "virtualbox")[0],
    )
})

macos_config.set_cross_compilers({
    "vagrant": VagrantBuildSystem
})

macos_config.set_file_abis({
    c.arch_x64: "x86_64",
    c.arch_x86: "x86_32",
    c.arch_arm64: "aarch_64",
})

#-----------------------------------------------------------------------
def build_node_js(config):
    return [
        "cd ./node",
        """./configure              \
            --without-intl          \
            --without-inspector     \
            --dest-cpu=$ARCH        \
            --without-snapshot      \
            --enable-static""",
        "make -j4",
    ]

macos_config.build_step(c.build_node_js, build_node_js)
#-----------------------------------------------------------------------
def build_v8(config):
    """Build V8 monolithic library natively on macOS"""
    import os
    
    # Determine V8 output directory based on architecture
    if config.arch == c.arch_arm64:
        v8_arch = "arm64"
        build_dir = "arm64.release"
    elif config.arch == c.arch_x64:
        v8_arch = "x64"
        build_dir = "x64.release"
    elif config.arch == c.arch_x86:
        v8_arch = "ia32"
        build_dir = "ia32.release"
    else:
        v8_arch = config.arch
        build_dir = config.arch + ".release"
    
    v8_out_dir = "macos." + v8_arch
    args_gn_source = "./v8/macos-" + v8_arch + "/args.gn"
    
    return [
        # Create V8 build directory if it doesn't exist
        "mkdir -p ./v8build",
        "cd ./v8build",
        
        # Fetch V8 if not already present
        """if [ ! -d "v8" ]; then
            echo "Fetching V8 source..."
            fetch v8
        fi""",
        
        "cd v8",
        
        # Checkout V8 12.4 branch
        """if ! git rev-parse --verify refs/branch-heads/12.4 >/dev/null 2>&1; then
            echo "Fetching V8 12.4 branch..."
            git fetch origin refs/branch-heads/12.4:refs/branch-heads/12.4
        fi""",
        
        "git checkout refs/branch-heads/12.4",
        
        # Sync dependencies
        "cd ..",
        'echo "target_os = [\'mac\']" >> .gclient',
        "gclient sync -D",
        
        "cd v8",
        
        # Create output directory and copy args.gn
        "mkdir -p out.gn/" + build_dir,
        "cp ../../" + args_gn_source + " out.gn/" + build_dir + "/args.gn",
        
        # Generate build files with GN
        "gn gen out.gn/" + build_dir,
        
        # Build V8 monolith with ninja (use -j4 to limit parallelism)
        "ninja -j4 -C out.gn/" + build_dir + " v8_monolith",
        
        # Copy output to expected location
        "cd ../..",
        "mkdir -p ./v8.out/" + v8_out_dir,
        "cp -R ./v8build/v8/include ./v8.out/",
        "cp ./v8build/v8/out.gn/" + build_dir + "/obj/libv8_monolith.a ./v8.out/" + v8_out_dir + "/",
        
        "echo 'V8 build complete! Library at: ./v8.out/" + v8_out_dir + "/libv8_monolith.a'",
    ]

macos_config.build_step(c.build_v8, build_v8)
#-----------------------------------------------------------------------
def build_j2v8_cmake(config):
    cmake_vars = cmu.setAllVars(config)
    
    # Determine V8 library directory based on architecture
    if config.arch == c.arch_arm64:
        v8_arch = "arm64"
    elif config.arch == c.arch_x64:
        v8_arch = "x64"
    elif config.arch == c.arch_x86:
        v8_arch = "ia32"
    else:
        v8_arch = config.arch
    
    V8_monolith_library_dir = "macos." + v8_arch

    # NOTE: uses Python string interpolation (see: https://stackoverflow.com/a/4450610)
    return \
        u.mkdir(u.cmake_out_dir) + \
        ["cd " + u.cmake_out_dir] + \
        u.rm("CMakeCache.txt CMakeFiles/") + \
        ["""cmake \
            -DJ2V8_MONOLITH_LIB_DIR={0} \
            -DCMAKE_BUILD_TYPE=Release \
            %(cmake_vars)s \
            ../../ \
        """.format(V8_monolith_library_dir)
        % locals()]

macos_config.build_step(c.build_j2v8_cmake, build_j2v8_cmake)
#-----------------------------------------------------------------------
macos_config.build_step(c.build_j2v8_jni, u.build_j2v8_jni)
#-----------------------------------------------------------------------
def build_j2v8_cpp(config):
    return [
        "cd " + u.cmake_out_dir,
        "make -j4",
    ]

macos_config.build_step(c.build_j2v8_cpp, build_j2v8_cpp)
#-----------------------------------------------------------------------
j.add_java_build_step(macos_config)
#-----------------------------------------------------------------------
j.add_java_test_step(macos_config)
#-----------------------------------------------------------------------
