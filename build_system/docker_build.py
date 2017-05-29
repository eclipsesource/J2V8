import subprocess
import sys
from shutil import copy2
from cross_build import BuildSystem

class DockerBuildSystem(BuildSystem):
    def clean(self, config, arch):
        try:
            self.exec_host_cmd("docker rm -f -v j2v8.$PLATFORM.$ARCH", config, arch)
        except subprocess.CalledProcessError:
            return

    def get_mount_string(self, mount_point, config, arch):
        return "-v " + mount_point.host_dir + ":" + mount_point.guest_dir

    def health_check(self, config, arch):
        try:
            self.exec_host_cmd("docker stats --no-stream", config, arch)
        except subprocess.CalledProcessError:
            sys.exit("ERROR: Failed Docker build-system health check, make sure Docker is available and running!")

    def pre_build(self, config, arch):
        print ("preparing " + config.platform + "@" + arch + " => " + config.name)

        # copy the maven  & gradle config file to the docker shared directory
        # this allows to download most of the maven dependencies for the build beforehand
        copy2("pom.xml", "./docker/shared")
        copy2("build.gradle", "./docker/shared")

        self.exec_host_cmd("docker build -f $PLATFORM/Dockerfile -t \"j2v8-$PLATFORM\" .", config, arch)

    def exec_build(self, config, arch, custom_cmd):
        print ("DOCKER building " + config.platform + "@" + arch + " => " + config.name)

        docker_run_str = self.inject_env("docker run -v $CWD:/j2v8 --name j2v8.$PLATFORM.$ARCH j2v8-$PLATFORM ", config, arch)
        build_cmds_str = self.inject_env("/bin/bash -c \"cd $BUILD_CWD; " + (custom_cmd or "; ".join(config.build(config, arch))) + "\"", config, arch)

        docker_str = docker_run_str + build_cmds_str

        self.exec_host_cmd(docker_str, config, arch)

    def post_build(self, config, arch):
        return
        #self.exec_host_cmd("docker cp j2v8.$PLATFORM.$ARCH:/build/jni/jniLibs $BUILD_CWD/src/main/", config, arch)
