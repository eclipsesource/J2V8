import build as b
import build_system.constants as c

class Object:
   def __init__(self, **attributes):
      self.__dict__.update(attributes)

# Android test
# b.execute_build({"target": c.target_android, "arch": c.arch_arm, "buildsteps": c.build_all, "node_enabled": True, "cross_compile": True})
# b.execute_build(Object(**{"target": c.target_android, "arch": c.arch_x86, "buildsteps": c.build_all, "node_enabled": True, "cross_compile": True}))

# MacOS test
# b.execute_build(c.target_macos, c.arch_x64, c.build_all, node_enabled = True, cross_compile = True)

# Win32 test
# b.execute_build(c.target_win32, c.arch_x64, c.build_all, node_enabled = True, cross_compile = False)

# b.execute_build(Object(**{"target": c.target_win32, "arch": c.arch_x64, "buildsteps": "j2v8java", "node_enabled": True, "cross_compile": False}))
# b.execute_build(Object(**{"target": c.target_linux, "arch": c.arch_x64, "buildsteps": c.build_j2v8_java, "node_enabled": True, "cross_compile": True}))

b.execute_build(Object(**{"target": c.target_android, "arch": c.arch_x86, "buildsteps": "j2v8java", "node_enabled": True, "cross_compile": False}))




#b.execute_build(c.target_macos, c.arch_x86, c.build_all, node_enabled = True, cross_compile = True)

# b.execute_build(c.target_linux, c.arch_x64, c.build_all, True, True)

# build Node.js only
# def build_njs(target, arch):
#     b.execute_build(target, arch, [c.build_node_js], node_enabled = True, cross_compile = True)

# build_njs(c.target_android, c.arch_arm)
# build_njs(c.target_android, c.arch_x86)

# build_njs(c.target_linux, c.arch_x86)
# build_njs(c.target_linux, c.arch_x64)

# # needs reboot here to turn Hyper-V off if Host-OS is Windows

# build_njs(c.target_macos, c.arch_x86)
# build_njs(c.target_macos, c.arch_x64)

# # needs reboot here to switch to Windows-Containers

# build_njs(c.target_win32, c.arch_x86)
# build_njs(c.target_win32, c.arch_x64)
