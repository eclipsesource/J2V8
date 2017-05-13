import build_constants as bc
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem

win32_config = PlatformConfig("win32", [bc.arch_x86, bc.arch_x64], DockerBuildSystem)
