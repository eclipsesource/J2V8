
import atexit
import re
import subprocess
import sys

from cross_build import BuildSystem
import constants as c
import build_utils as utils

class DockerBuildSystem(BuildSystem):
    def clean(self, config):
        try:
            self.exec_host_cmd("docker rm -f -v j2v8.$PLATFORM.$ARCH", config)
        except subprocess.CalledProcessError:
            return

    def health_check(self, config):
        try:
            # general docker availability check
            self.exec_host_cmd("docker stats --no-stream", config)

            # NOTE: the additional newlines are important for the regex matching
            version_str = utils.execute_to_str("docker version") + "\n\n"

            server_match = re.search(r"Server:(.*)\n\n", version_str + "\n\n", re.DOTALL)

            if (server_match is None or server_match.group(1) is None):
                sys.exit("ERROR: Unable to determine docker server version from version string: \n\n" + version_str)

            version_match = re.search(r"^ OS/Arch:\s+(.*)$", server_match.group(1), re.MULTILINE)

            if (version_match is None):
                sys.exit("ERROR: Unable to determine docker server platform from version string: \n\n" + version_str)

            docker_version = version_match.group(1)

            docker_req_platform = "windows" if utils.is_win32(config.platform) else "linux"

            # check if the docker engine is running the expected container platform (linux or windows)
            if (docker_req_platform not in docker_version):
                sys.exit("ERROR: docker server must be using " + docker_req_platform + " containers, instead found server version using: " + docker_version)

        except subprocess.CalledProcessError:
            sys.exit("ERROR: Failed Docker build-system health check, make sure Docker is available and running!")

    def pre_build(self, config):
        print ("preparing " + config.platform + "@" + config.arch + " => " + config.name)

        docker_stop_str = self.inject_env("docker stop j2v8.$PLATFORM.$ARCH", config)

        def cli_exit_event():
            if (config.no_shutdown):
                return

            print "Waiting for docker process to exit..."
            self.exec_host_cmd(docker_stop_str, config)

        atexit.register(cli_exit_event)

        self.exec_host_cmd("docker build -f $PLATFORM/Dockerfile -t \"j2v8-$PLATFORM\" .", config)

    def exec_build(self, config):
        print ("DOCKER running " + config.platform + "@" + config.arch + " => " + config.name)

        is_win32 = utils.is_win32(config.platform)

        mount_point = "C:/j2v8" if is_win32 else "/j2v8"
        shell_invoke = "cmd /C" if is_win32 else "/bin/bash -c"
        cmd_separator = "&&" if is_win32 else ";"

        build_cmd = config.custom_cmd or (cmd_separator + " ").join(config.build(config))

        memory_option = ""

        # NOTE: the --memory 3g setting is imporant for windows docker builds,
        # since the windows docker engine defaults to a 1gb limit which is not enough to run the Node.js build with MSBuild
        if (utils.is_win32(config.platform)):
            memory_option = "--memory 3g"

        docker_run_str = "docker run " + memory_option + " --privileged -P -v $CWD:" + mount_point + \
            " --name j2v8.$PLATFORM.$ARCH j2v8-$PLATFORM " + shell_invoke + " \"cd $BUILD_CWD" + cmd_separator + " " + build_cmd + "\""

        docker_run_str = self.inject_env(docker_run_str, config)

        print docker_run_str

        self.exec_host_cmd(docker_run_str, config)

    def post_build(self, config):
        return
