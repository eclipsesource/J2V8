from abc import ABCMeta, abstractmethod
import commands
import os
import sys
from shutil import copy2
import build_settings as s
import build_utils as utils

class PlatformConfig():
    def __init__(self, name, architectures):
        self.name = name
        self.architectures = architectures
        self.file_abis = {}
        self.steps = {}
        self.cross_compilers = {}
        self.cross_configs = {}

    def build_step(self, target, build_fn):
        self.steps[target] = BuildStep(
            name=target,
            platform=self.name,
            build=build_fn,
        )

    def set_cross_compilers(self, compilers_decl):
        self.cross_compilers = compilers_decl

    def cross_compiler(self, cross_host_name):
        compiler = self.cross_compilers.get(cross_host_name)

        if (not compiler):
            sys.exit("ERROR: internal error while looking for cross-compiler: " + cross_host_name)

        return compiler()

    def set_cross_configs(self, cross_configs_decl):
        self.cross_configs = cross_configs_decl

    def set_file_abis(self, abis_decl):
        self.file_abis = abis_decl

    def file_abi(self, arch):
        file_abi = self.file_abis.get(arch)
        return file_abi if not file_abi is None else arch

class BuildStep(object):
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

        utils.execute(cmd, dir)

    def exec_cmd(self, cmd, config):
        cmd = self.inject_env(cmd, config)
        dir = None

        if (config.build_cwd is not None):
            dir = self.inject_env(config.build_cwd, config)

        utils.execute(cmd, dir)

    def inject_env(self, cmd, config):
        build_cwd = utils.get_cwd()
        vendor = config.vendor

        return (cmd
            # global config variables
            .replace("$NODE_VERSION", s.NODE_VERSION)
            .replace("$J2V8_VERSION", s.J2V8_VERSION)
            .replace("$J2V8_FULL_VERSION", s.J2V8_FULL_VERSION)

            # build specific variables
            .replace("$BUILD_CWD", config.build_cwd or build_cwd)
            .replace("$HOST_CWD", config.host_cwd or "")
            .replace("$CWD", build_cwd)
            .replace("$PLATFORM", config.platform)
            .replace("$ARCH", config.arch)
            .replace("$FILE_ABI", config.file_abi)
            .replace("$LIB_EXT", utils.platform_libext(config))

            # Vendor can be an optional part,
            # therefore some additional tricks in the string replacement are needed here
            .replace(".$VENDOR", "." + vendor if vendor else "")
            .replace("-$VENDOR", "-" + vendor if vendor else "")
            .replace("$VENDOR.", vendor + "." if vendor else "")
            .replace("$VENDOR-", vendor + "-" if vendor else "")
            .replace("$VENDOR", config.vendor or "")
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
