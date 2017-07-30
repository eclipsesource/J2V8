
# see: https://cmake.org/cmake/help/v2.8.8/cmake.html#opt:-Dvar:typevalue
def setVar(var, value, type = "STRING"):
    return " -D%(var)s:%(type)s=%(value)s " % locals()

def setTargetArch(config):
    return setVar("J2V8_TARGET_ARCH", config.file_abi)

def setNodeEnabled(config):
    return setVar("J2V8_NODE_ENABLED", "TRUE" if config.node_enabled else "FALSE", "BOOL")

def setVendor(config):
    return setVar("J2V8_VENDOR", config.vendor) if config.vendor else ""

def setCrossCompile(config):
    return setVar("J2V8_CROSS_COMPILE", "TRUE", "BOOL") if config.cross_agent else ""

def setToolchain(toolchain_file_path):
    return setVar("CMAKE_TOOLCHAIN_FILE", toolchain_file_path)

def setWin32PdbDockerFix(config):
    return setVar("J2V8_WIN32_PDB_DOCKER_FIX", "TRUE", "BOOL") if config.cross_agent == "docker" else ""

def setAllVars(config):
    return \
        setCrossCompile(config) + \
        setTargetArch(config) + \
        setVendor(config) + \
        setNodeEnabled(config)
