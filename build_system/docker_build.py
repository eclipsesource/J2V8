import subprocess
import sys
from cross_build import BuildSystem
import constants as c

class DockerBuildSystem(BuildSystem):
    def clean(self, config):
        try:
            self.exec_host_cmd("docker rm -f -v j2v8.$PLATFORM.$ARCH", config)
        except subprocess.CalledProcessError:
            return

    def health_check(self, config):
        try:
            self.exec_host_cmd("docker stats --no-stream", config)
        except subprocess.CalledProcessError:
            sys.exit("ERROR: Failed Docker build-system health check, make sure Docker is available and running!")

    def pre_build(self, config):
        print ("preparing " + config.platform + "@" + config.arch + " => " + config.name)

        self.exec_host_cmd("docker build -f $PLATFORM/Dockerfile -t \"j2v8-$PLATFORM\" .", config)

    def exec_build(self, config):
        print ("DOCKER building " + config.platform + "@" + config.arch + " => " + config.name)

        mount_point = "C:/j2v8" if config.platform == c.target_win32 else "/j2v8"
        shell_invoke = "cmd /C" if config.platform == c.target_win32 else "/bin/bash -c"
        cmd_separator = "&&" if config.platform == c.target_win32 else ";"

        build_cmd = config.custom_cmd or (cmd_separator + " ").join(config.build(config))

        platform_cmd = "docker run --privileged -P -v $CWD:" + mount_point + \
            " --name j2v8.$PLATFORM.$ARCH j2v8-$PLATFORM " + shell_invoke + " \"cd $BUILD_CWD" + cmd_separator + " " + build_cmd + "\""

        docker_run_str = self.inject_env(platform_cmd, config)

        print docker_run_str

        self.exec_host_cmd(docker_run_str, config)

    def post_build(self, config):
        return
