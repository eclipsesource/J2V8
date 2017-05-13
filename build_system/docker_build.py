import subprocess
import sys
from cross_build import BuildSystem

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

        build_cmds_str = self.inject_env("cd $BUILD_CWD; " + (custom_cmd or " && ".join(config.build(config, arch))), config, arch)

        self.exec_cmd("docker run $MOUNTS --name j2v8.$PLATFORM.$ARCH j2v8-$PLATFORM-$ARCH " + build_cmds_str, config, arch)

    def post_build(self, config, arch):
        self.exec_cmd("docker cp j2v8.$PLATFORM.$ARCH:/build/jni/jniLibs $BUILD_CWD/src/main/", config, arch)
