"""Contains the fundamental data-structures that are used for the build-process"""

from abc import ABCMeta, abstractmethod
import subprocess
import os
import sys
from shutil import copy2
from . import build_settings as s
from . import build_utils as utils
from . import shared_build_steps as sbs

class PlatformConfig():
    """Configuration container for all values that are defined for a single target-platform"""
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
            utils.cli_exit("ERROR: internal error while looking for cross-compiler: " + cross_host_name)

        return compiler()

    def set_cross_configs(self, cross_configs_decl):
        self.cross_configs = cross_configs_decl

    def set_file_abis(self, abis_decl):
        self.file_abis = abis_decl

    def file_abi(self, arch):
        file_abi = self.file_abis.get(arch)
        return file_abi if not file_abi is None else arch

class BuildStep(object):
    """Configuration capsule for all values that are defined for a well-defined step in the build pipeline"""
    def __init__(self, name, platform, build = [], build_cwd = None, host_cwd = None, v8_cwd = None):
        self.name = name
        self.platform = platform
        self.build = build
        self.build_cwd = build_cwd
        self.host_cwd = host_cwd
        self.v8_cwd = v8_cwd
        self.custom_cmd = None

class BuildSystem:
    """The functional compositor and abstract base-class for any concrete build-system implementation"""
    __metaclass__ = ABCMeta

    def prepare_build(self, config):
        # perform the health check for this build-system first
        self.health_check(config)

        # clean previous build outputs
        self.clean(config)

    def build_v8(self, config):
        self.prepare_build(config)
        
        # execute V8 build stage
        self.exec_v8_build(config)

        # store V8 build output
        utils.store_v8_output(self.get_v8_image_name(config), config)

    def build(self, config):
        self.prepare_build(config)

        # copy the maven / gradle config files to the docker shared directory
        # this allows Dockerfiles to pre-fetch most of the maven / gradle dependencies before the actual build
        # and store downloaded maven / gradle dependencies inside the generated docker images
        # (results in faster builds/less network traffic)
        copy2("build.gradle", "./docker/shared")
        copy2("src/main/AndroidManifest.xml", "./docker/android/AndroidManifest.xml")
        # use the original pom.xml, but with some never changing dummy parameter values.
        # this avoids unnecessary rebuilding of docker images (some pom.xml changes are mandatory during the J2V8 build)
        sbs.apply_maven_null_settings(target_pom_path="./docker/shared/pom.xml")

        # execute all the build stages
        self.pre_build(config)
        self.exec_build(config)
        self.post_build(config)

    def exec_v8_cmd(self, cmd, config):
        """Execute a shell-command on the host system (injects $CWD as the location of the J2V8 source directory"""
        self.__exec_cmd_core(cmd, config, config.v8_cwd)

    def exec_host_cmd(self, cmd, config):
        """Execute a shell-command on the host system (injects $CWD as the location of the J2V8 source directory"""
        self.__exec_cmd_core(cmd, config, config.host_cwd)

    def exec_cmd(self, cmd, config):
        """
        Execute a shell-command in the current shell environment (could be native or inside a virtualized system)
        On the native host-system, $CWD will be set to the location of the J2V8 source directory.
        Running inside a virtualized system, $CWD will be set to the path configured in the cross-compiler settings.
        """
        self.__exec_cmd_core(cmd, config, config.build_cwd)

    def __exec_cmd_core(self, cmd, config, cwd):
        cmd = self.inject_env(cmd, config)

        if (cwd is not None):
            # inject env-vars in the given working-directory path
            cwd = self.inject_env(cwd, config)

        utils.execute(cmd, cwd)

    def inject_env(self, cmd, config):
        """
        Grab values for often used properties from the config object
        and perform variable substitution on the given cmd string.
        """
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
            .replace("$VENDOR", vendor or "")
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
