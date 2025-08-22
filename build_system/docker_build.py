
import atexit
import re
import subprocess
import sys
import platform

from .build_structures import BuildSystem, BuildStep
from . import constants as c
from . import build_utils as utils
from . import docker_configs as dkr_cfg

class DockerBuildStep(BuildStep):
    def __init__(self, platform, build_cwd = None, host_cwd = None, v8_cwd = None):
        super(DockerBuildStep, self).__init__("docker-build-host", platform, None, build_cwd, host_cwd, v8_cwd)

class DockerBuildSystem(BuildSystem):
    def clean(self, config):
        try:
            container_name = self.get_container_name(config)
            self.exec_host_cmd("docker rm -f -v " + container_name, config)
        except subprocess.CalledProcessError:
            return

    def health_check(self, config):
        print ("Verifying Docker build-system status...")
        try:
            # general docker availability check
            self.exec_host_cmd("docker --version", config)

            # check the currently active container technology (linux vs. windows containers)
            # NOTE: the additional newlines are important for the regex matching
            version_str = utils.execute_to_str("docker version") + "\n\n"

            docker_version_match = re.search(r"Client:(.*)\n\n", version_str + "\n\n", re.DOTALL)

            if (docker_version_match is None or docker_version_match.group(1) is None):
                utils.cli_exit("ERROR: Unable to determine docker server version from version string: \n\n" + version_str)

            version_match = re.search(r"OS/Arch:\s+(.*)$", docker_version_match.group(1), re.MULTILINE)

            if (version_match is None):
                utils.cli_exit("ERROR: Unable to determine docker server platform from version string: \n\n" + version_str)

            docker_version = version_match.group(1)

            docker_req_platform = "windows" if utils.is_win32(config.platform) else "linux"
            if (platform.system() == "Darwin"):
                docker_req_platform = "darwin/arm64"

            # check if the docker engine is running the expected container platform (linux or windows)
            if (docker_req_platform not in docker_version):
                utils.cli_exit("ERROR: docker server must be using " + docker_req_platform + " containers, instead found server version using: " + docker_version)

        except subprocess.CalledProcessError:
            utils.cli_exit("ERROR: Failed Docker build-system health check, make sure Docker is available and running!")

    def get_v8_image_name(self, config):
        return "v8-" + config.platform + "-" + config.arch

    def get_image_name(self, config):
        return "j2v8-$VENDOR-$PLATFORM"

    def get_container_name(self, config):
        return "j2v8.$VENDOR.$PLATFORM.$ARCH"
        
    def exec_v8_build(self, config):
        print ("V8 build preparing " + config.platform + "@" + config.arch + " => " + config.name)

        args_str = ""

        def build_arg(name, value):
            return (" --build-arg " + name + "=" + value) if value else ""

        def target_os(value):
            return build_arg("target_os", value)

        def target_cpu(value):
            return build_arg("target_cpu", value)
        
        # if we are building with docker
        # and a specific vendor was specified for the build
        # and no custom sys-image was specified ...
        if (config.docker and config.vendor and not config.sys_image):
            vendor_default_image = dkr_cfg.vendor_default_images.get(config.vendor)

        dest_cpu= config.arch
        if config.arch == c.arch_x86_64:
            dest_cpu = c.arch_x64
        elif config.arch == c.arch_x86:
            dest_cpu = 'ia32'

        args_str += target_os(config.platform)
        args_str += target_cpu(dest_cpu)

        image_name = self.get_v8_image_name(config)

        print ("Building V8 docker image: " + config.inject_env(image_name))
        self.exec_v8_cmd("docker build " + args_str + " -f Dockerfile -t \"" + image_name + "\" . ", config)

    def pre_build(self, config):
        print ("preparing " + config.platform + "@" + config.arch + " => " + config.name)

        container_name = self.get_container_name(config)
        docker_stop_str = self.inject_env("docker stop " + container_name, config)

        def cli_exit_event():
            if config.no_shutdown:
                print ("INFO: Docker J2V8 container will continue running...")
                return

            print ("Waiting for docker process to exit...")
            self.exec_host_cmd(docker_stop_str, config)

        atexit.register(cli_exit_event)

        args_str = ""

        def build_arg(name, value):
            return (" --build-arg " + name + "=" + value) if value else ""

        def sys_image_arg(value):
            return build_arg("sys_image", value)

        def vendor_arg(value):
            return build_arg("vendor", value)

        def keystore_arg(value):
            return build_arg("KEYSTORE", value)

        def keystorepassword_arg(value):
            return build_arg("KEYSTORE_PASSWORD", value)

        # use custom sys-image if it was specified by the user
        args_str += sys_image_arg(config.sys_image)

        # if we are building with docker
        # and a specific vendor was specified for the build
        # and no custom sys-image was specified ...
        if (config.docker and config.vendor and not config.sys_image):
            vendor_default_image = dkr_cfg.vendor_default_images.get(config.vendor)

            # ... then use the default image for that vendor if available
            args_str += sys_image_arg(vendor_default_image)

        # pass a specified vendor string to the docker build
        args_str += vendor_arg(config.vendor)

        args_str += keystore_arg("$KEYSTORE")
        args_str += keystorepassword_arg("$KEYSTORE_PASSWORD")

        image_name = self.get_image_name(config)

        print ("Building docker image: " + config.inject_env(image_name))
        self.exec_host_cmd("docker build " + args_str + " -f $PLATFORM/Dockerfile -t \"" + image_name + "\" . ", config)

    def exec_build(self, config):
        print ("DOCKER running " + config.platform + "@" + config.arch + " => " + config.name)

        is_win32 = utils.is_win32(config.platform)

        mount_point = "C:/j2v8" if is_win32 else "/j2v8"
        shell_invoke = "cmd /C" if is_win32 else "/bin/bash -c"
        cmd_separator = "&&" if is_win32 else ";"

        build_cmd = config.custom_cmd or (cmd_separator + " ").join(config.build(config))

        extra_options = ""

        # NOTE: the --memory 3g setting is imporant for windows docker builds,
        # since the windows docker engine defaults to a 1gb limit which is not enough to run the Node.js build with MSBuild
        if (utils.is_win32(config.platform)):
            extra_options = "--memory 3g"
        else:
            extra_options = "--privileged"

        image_name = self.get_image_name(config)
        container_name = self.get_container_name(config)

        docker_run_str = "docker run " + extra_options + " -e KEY_ID=$KEY_ID -e KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD -e MAVEN_REPO_USER=$MAVEN_REPO_USER -e MAVEN_REPO_PASS=$MAVEN_REPO_PASS -P -v $CWD:" + mount_point + \
            " --name " + container_name + " " + image_name + " " + shell_invoke + " \"cd $BUILD_CWD" + cmd_separator + " " + build_cmd + "\""

        docker_run_str = self.inject_env(docker_run_str, config)

        print (docker_run_str)

        self.exec_host_cmd(docker_run_str, config)

    def post_build(self, config):
        return
