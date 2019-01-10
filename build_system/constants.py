"""Commonly used string constants (platforms, architectures, vendors, build-steps)"""

# target platforms
target_android = 'android'
target_linux = 'linux'
target_macos = 'macos'
target_win32 = 'win32'

vendor_alpine = 'alpine'
vendor_debian = 'debian'

# target architectures
arch_x86 = 'x86'
arch_x64 = 'x64'
arch_x86_64 = 'x86_64'
arch_arm = 'arm'
arch_arm64 = 'arm64'

# atomic build-steps
build_node_js = 'nodejs'
build_j2v8_cmake = 'j2v8cmake'
build_j2v8_jni = 'j2v8jni'
build_j2v8_cpp = 'j2v8cpp'
build_j2v8_optimize = 'j2v8optimize'
build_j2v8_java = 'j2v8java'
build_j2v8_test = 'j2v8test'

# aliases
build_java = 'java'
build_test = 'test'

# composites
build_all = 'all'
build_native = 'native'
build_j2v8 = 'j2v8'
