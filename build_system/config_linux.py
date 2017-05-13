import constants as c
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem

linux_config = PlatformConfig("linux", [c.arch_x86, c.arch_x64], DockerBuildSystem)
