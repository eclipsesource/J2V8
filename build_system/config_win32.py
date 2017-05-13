import constants as c
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem

win32_config = PlatformConfig("win32", [c.arch_x86, c.arch_x64], DockerBuildSystem)
