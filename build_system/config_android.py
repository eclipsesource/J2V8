import constants as c
from cross_build import BuildConfig, PlatformConfig
from docker_build import DockerBuildSystem

android_config = PlatformConfig("android", [c.arch_x86, c.arch_arm], DockerBuildSystem)
