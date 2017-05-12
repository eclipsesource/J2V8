from __future__ import print_function # Only Python 2.x
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

class BuildConfig:
    def __init__(self, name, platform, mounts = [], build = [], build_cwd = None, host_cwd = None):
        self.name = name
        self.platform = platform
        self.mounts = mounts
        # self.prebuild = prebuild
        self.build = build
        # self.postbuild = postbuild
        self.build_cwd = build_cwd
        self.host_cwd = host_cwd

    # def build_id(self, arch):
    #     return "j2v8." + self.platform + "." + arch

    # def kebab_id(self, arch):
    #     return self.build_id(arch).replace(".", "-")

class MountPoint:
    def __init__(self, host_dir, guest_dir):
        self.host_dir = host_dir
        self.guest_dir = guest_dir

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

class DockerBuildSystem(BuildSystem):
    def clean(self, config, arch):
        try:
            self.exec_cmd("docker rm -f $PLATFORM@$ARCH", config, arch)
        except subprocess.CalledProcessError:
            return

    def get_mount_string(self, mount_point, config, arch):
        return "-v " + mount_point.host_dir + ":" + mount_point.guest_dir

    def health_check(self, config, arch):
        try:
            self.exec_cmd("docker stats --no-stream", config, arch)
        except subprocess.CalledProcessError:
            sys.exit("ERROR: Failed Docker build-system health check, make sure Docker is available and running!")

    def pre_build(self, config, arch):
        print ("preparing " + config.platform + "@" + arch + " => " + config.name)
        self.exec_cmd("docker build -t \"j2v8-$PLATFORM-$ARCH\" -f docker/Dockerfile.$PLATFORM $BUILD_CWD", config, arch)

    def exec_build(self, config, arch, custom_cmd):
        print ("DOCKER building " + config.platform + "@" + arch + " => " + config.name)

        build_cmds_str = self.inject_env("cd $BUILD_CWD; " + (custom_cmd or " && ".join(config.build)), config, arch)

        self.exec_cmd("docker run $MOUNTS --name j2v8.$PLATFORM.$ARCH j2v8-$PLATFORM-$ARCH " + build_cmds_str, config, arch)

    def post_build(self, config, arch):
        self.exec_cmd("docker cp j2v8.$PLATFORM.$ARCH:/build/jni/jniLibs $BUILD_CWD/src/main/", config, arch)

class VagrantBuildSystem(BuildSystem):
    def clean(self, config, arch):
        return

    def get_mount_string(self, mount_point):
        return

    def health_check(self, config, arch):
        try:
            self.exec_host_cmd("vagrant global-status", config, arch)
        except subprocess.CalledProcessError:
            sys.exit("ERROR: Failed Vagrant build-system health check, make sure Vagrant is available and running!")

    def pre_build(self, config, arch):
        self.exec_host_cmd("vagrant up", config, arch)

    def exec_build(self, config, arch, custom_cmd):
        #[self.exec_cmd(cmd, config, arch) for cmd in config.build]
        # self.exec_cmd("vagrant ssh -c 'cd ./build/node; ./configure --without-intl --without-inspector --dest-cpu=$ARCH --without-snapshot --enable-static", config, arch)
        # self.exec_cmd("vagrant ssh -c 'cd ./build/node; make -j4'", config, arch)
        print ("VAGRANT building " + config.platform + "@" + arch + " => " + config.name)

        # TODO
        build_cmds_str = self.inject_env("cd $BUILD_CWD; " + (custom_cmd or "; ".join(config.build)), config, arch)

        self.exec_host_cmd("vagrant ssh -c '" + build_cmds_str + "'", config, arch)

    def post_build(self, config, arch):
        self.exec_host_cmd("vagrant halt", config, arch)

class ShellBuildSystem(BuildSystem):
    def clean(self, config, arch):
        return

    def get_mount_string(self, mount_point):
        return

    def health_check(self, config, arch):
        try:
            self.exec_cmd("sh --version", config, arch)
        except subprocess.CalledProcessError:
            sys.exit("ERROR: Failed Shell build-system health check, make sure Vagrant is available and running!")

    def pre_build(self, config, arch):
        return

    def exec_build(self, config, arch, custom_cmd):
        print ("SHELL building " + config.platform + "@" + arch + " => " + config.name)

        build_cmds_str = self.inject_env(custom_cmd or " && ".join(config.build), config, arch)

        self.exec_cmd(build_cmds_str, config, arch)

    def post_build(self, config, arch):
        return

docker = DockerBuildSystem()
vagrant = VagrantBuildSystem()

docker_builds = [
    #------------------------------------------------------------------------------------------
    # BuildConfig("android", "x86",
    # [
    #     MountPoint("$CWD/node.out", "/build/node")
    # ],
    # "android-gcc-toolchain x86 --api 15 --host gcc-lpthread -C sh -c \"cd jni && ndk-build\""),
    #------------------------------------------------------------------------------------------
    # BuildConfig("linux",
    # [
    #     MountPoint("$CWD/node.out", "/build/node"),
    #     MountPoint("$CWD", "/build/."),
    # ]),
    #------------------------------------------------------------------------------------------
]

vagrant_builds = [
    #------------------------------------------------------------------------------------------
    # BuildConfig("macos"),
    #------------------------------------------------------------------------------------------
]

# docker.build(docker_builds, "x64")
# vagrant.build(vagrant_builds)

# docker = DockerBuildSystem()
# vagrant = VagrantBuildSystem()

# android_x86 = BuildConfig("android", "x86",
#     [
#         MountPoint("$CWD/node.out", "/build/node")
#     ],
#     "android-gcc-toolchain x86 --api 15 --host gcc-lpthread -C sh -c \"cd jni && ndk-build\"")

# linux_x64 = BuildConfig("linux", "x64",
#     [
#         MountPoint("$CWD/node.out", "/build/node"),
#         MountPoint("$CWD", "/build/."),
#     ])

# macos_x64 = BuildConfig("macos", "x64")

# docker_builds = [
#     android_x86,
#     linux_x64,
# ]

# vagrant_builds = [
#     macos_x64,
# ]

# #execute_build(docker_builds, docker)
# execute_build(vagrant_builds, vagrant)
