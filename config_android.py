import build_constants as bc
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem

android_config = PlatformConfig("android", [bc.arch_x86, bc.arch_arm], DockerBuildSystem)
