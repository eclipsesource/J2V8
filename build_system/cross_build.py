from abc import ABCMeta, abstractmethod
import commands
import os
import subprocess
import sys
from shutil import copy2
import build_system.build_utils as utils

def execute(cmd, cwd = None):
    popen = subprocess.Popen(cmd, universal_newlines=True, shell=True, cwd=cwd)
    return_code = popen.wait()
    if return_code:
        raise subprocess.CalledProcessError(return_code, cmd)

class PlatformConfig():
    def __init__(self, name, architectures, cross_compiler):
        self.name = name
        self.architectures = architectures
        self.cross_compiler = cross_compiler
        self.steps = {}

    def build_step(self, target, build_fn):
        self.steps[target] = BuildStep(
            name=target,
            platform=self.name,
            build=build_fn,
        )

    def cross_config(self, cross_config):
        self.steps['cross'] = cross_config

class BuildStep:
    def __init__(self, name, platform, build = [], build_cwd = None, host_cwd = None):
        self.name = name
        self.platform = platform
        self.build = build
        self.build_cwd = build_cwd
        self.host_cwd = host_cwd
        self.custom_cmd = None

class BuildSystem:
    __metaclass__ = ABCMeta

    def build(self, config):
        # perform the health check for the build system first
        self.health_check(config)

        # clean previous build outputs
        self.clean(config)

        # copy the maven  & gradle config file to the docker shared directory
        # this allows to pre-fetch most of the maven dependencies before the actual build (e.g. into docker images)
        copy2("pom.xml", "./docker/shared")
        copy2("build.gradle", "./docker/shared")
        copy2("src/main/AndroidManifest.xml", "./docker/android/AndroidManifest.xml")

        # execute all the build stages
        self.pre_build(config)
        self.exec_build(config)
        self.post_build(config)

    def exec_host_cmd(self, cmd, config):
        cmd = self.inject_env(cmd, config)
        dir = None

        if (config.host_cwd is not None):
            dir = self.inject_env(config.host_cwd, config)

        execute(cmd, dir)

    def exec_cmd(self, cmd, config):
        cmd = self.inject_env(cmd, config)
        dir = None

        if (config.build_cwd is not None):
            dir = self.inject_env(config.build_cwd, config)

        execute(cmd, dir)

    def inject_env(self, cmd, config):
        build_cwd = utils.get_cwd()

        return (cmd
            .replace("$BUILD_CWD", config.build_cwd or build_cwd)
            .replace("$HOST_CWD", config.host_cwd or "")
            .replace("$CWD", build_cwd)
            .replace("$PLATFORM", config.platform)
            .replace("$ARCH", config.arch)
        )

    @abstractmethod
    def health_check(self, config):
        pass

    @abstractmethod
    def clean(self, config):
        pass

    @abstractmethod
    def pre_build(self, config):
        pass

    @abstractmethod
    def exec_build(self, config):
        pass

    @abstractmethod
    def post_build(self, config):
        pass
