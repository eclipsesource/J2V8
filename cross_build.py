from abc import ABCMeta, abstractmethod
import commands
import os
import subprocess
import sys

cwd = os.path.dirname(os.path.realpath(__file__)).replace("\\", "/")

def execute(cmd, cwd = None):
    popen = subprocess.Popen(cmd, universal_newlines=True, shell=True, cwd=cwd)
    # for stdout_line in iter(popen.stdout.readline, ""):
    #     yield stdout_line 
    # popen.stdout.close()
    return_code = popen.wait()
    if return_code:
        raise subprocess.CalledProcessError(return_code, cmd)

class PlatformConfig():
    def __init__(self, platform_name, architectures, cross_compiler):
        self.platform_name = platform_name
        self.architectures = architectures
        self.cross_compiler = cross_compiler
        self.configs = {}

    def build_step(self, target, build_fn):
        self.configs[target] = BuildConfig(
            name=target,
            platform=self.platform_name,
            build=build_fn,
        )

    def cross_config(self, cross_config):
        self.configs['cross'] = cross_config

class BuildConfig:
    def __init__(self, name, platform, mounts = [], build = [], build_cwd = None, host_cwd = None):
        self.name = name
        self.platform = platform
        self.mounts = mounts
        self.build = build
        self.build_cwd = build_cwd
        self.host_cwd = host_cwd

class BuildSystem:
    __metaclass__ = ABCMeta

    def build(self, config, arch, custom_cmd = None):
        # perform the health check for the build system first
        self.health_check(config, arch)
            
        # clean previous build outputs
        self.clean(config, arch)
        
        # execute all the builds
        self.__build(config, arch, custom_cmd)

    def __build(self, config, arch, custom_cmd):
        self.pre_build(config, arch)
        self.exec_build(config, arch, custom_cmd)
        self.post_build(config, arch)

    def exec_host_cmd(self, cmd, config, arch):
        cmd = self.inject_env(cmd, config, arch)
        dir = None
        
        if (config.host_cwd is not None):
            dir = self.inject_env(config.host_cwd, config, arch)
        
        execute(cmd, dir)

    def exec_cmd(self, cmd, config, arch):
        cmd = self.inject_env(cmd, config, arch)
        dir = None
        
        if (config.build_cwd is not None):
            dir = self.inject_env(config.build_cwd, config, arch)
        
        execute(cmd, dir)

        #subprocess.call(cmd, shell=True)
        #os.popen(cmd).read()

    def inject_env(self, cmd, config, arch):
        mounts = [self.get_mount_string(x) for x in config.mounts]
        mounts_str = " ".join(mounts)

        return (cmd
            # substitute variables that can contain variables first
            .replace("$MOUNTS", mounts_str)
            # substitute atomic variables later
            .replace("$CWD", cwd)
            .replace("$BUILD_CWD", config.build_cwd or cwd)
            .replace("$HOST_CWD", config.host_cwd or "")
            .replace("$PLATFORM", config.platform)
            .replace("$ARCH", arch)
        )

    @abstractmethod
    def get_mount_string(self, mount_point):
        pass

    @abstractmethod
    def health_check(self, config, arch):
        pass

    @abstractmethod
    def clean(self, config, arch):
        pass

    @abstractmethod
    def pre_build(self, config, arch):
        pass

    @abstractmethod
    def exec_build(self, config, arch, custom_cmd):
        pass

    @abstractmethod
    def post_build(self, config, arch):
        pass
