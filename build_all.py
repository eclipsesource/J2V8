import build_platform as b

# b.execute_build(b.target_macos, b.arch_x86, b.build_all, node_enabled = True, cross_compile = True)
b.execute_build(b.target_macos, b.arch_x64, b.build_all, node_enabled = True, cross_compile = True)

# b.execute_build(b.target_linux, b.arch_x64, b.build_all, True, True)

# build Node.js only
# def build_njs(target, arch):
#     b.execute_build(target, arch, [b.build_node_js], node_enabled = True, cross_compile = True)

# build_njs(b.target_android, b.arch_arm)
# build_njs(b.target_android, b.arch_x86)

# build_njs(b.target_linux, b.arch_x86)
# build_njs(b.target_linux, b.arch_x64)

# # needs reboot here to turn Hyper-V off if Host-OS is Windows

# build_njs(b.target_macos, b.arch_x86)
# build_njs(b.target_macos, b.arch_x64)

# # needs reboot here to switch to Windows-Containers

# build_njs(b.target_win32, b.arch_x86)
# build_njs(b.target_win32, b.arch_x64)
