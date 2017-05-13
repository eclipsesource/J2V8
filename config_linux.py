import build_constants as bc
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem

linux_config = PlatformConfig("linux", [bc.arch_x86, bc.arch_x64], DockerBuildSystem)
