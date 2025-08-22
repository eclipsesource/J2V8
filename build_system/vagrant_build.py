import atexit
import subprocess
import sys
from . import build_utils as utils
from .build_structures import BuildSystem, BuildStep
from . import shared_build_steps as u

class VagrantBuildStep(BuildStep):
    def __init__(self, platform, build_cwd = None, host_cwd = None, pre_build_cmd = None):
        super(VagrantBuildStep, self).__init__("vagrant-build-host", platform, None, build_cwd, host_cwd)
        self.pre_build_cmd = pre_build_cmd

class VagrantBuildSystem(BuildSystem):
    def clean(self, config):
        return

    def health_check(self, config):
        print ("Verifying Vagrant build-system status...")
        try:
            self.exec_host_cmd("vagrant --version", config)
        except subprocess.CalledProcessError:
            utils.cli_exit("ERROR: Failed Vagrant build-system health check, make sure Vagrant is available and running!")

    def pre_build(self, config):
        vagrant_start_cmd = "vagrant up"

        if (config.sys_image):
            vagrant_start_cmd = u.setEnvVar("VAGRANT_SYS_IMAGE", config.sys_image)[0] + utils.host_cmd_sep() + vagrant_start_cmd

        if (config.pre_build_cmd):
            vagrant_start_cmd = config.pre_build_cmd + utils.host_cmd_sep() + vagrant_start_cmd

        def cli_exit_event():
            if (config.no_shutdown):
                print ("INFO: Vagrant J2V8 machine will continue running...")
                return

            print ("Waiting for vagrant virtual-machine to exit...")
            self.exec_host_cmd("vagrant halt", config)

        atexit.register(cli_exit_event)

        self.exec_host_cmd(vagrant_start_cmd, config)

    def exec_build(self, config):
        print ("VAGRANT running " + config.platform + "@" + config.arch + " => " + config.name)

        vagrant_run_cmd = None

        if (utils.is_win32(config.platform)):
            cmd_sep = "; "
            build_cmd = config.custom_cmd or cmd_sep.join(config.build(config))
            build_cmd = self.inject_env("cd $BUILD_CWD" + cmd_sep + build_cmd, config)
            vagrant_run_cmd = "vagrant powershell -c \"Invoke-Command { " + build_cmd + " } -ErrorAction Stop\""
        else:
            cmd_sep = "; "
            build_cmd = config.custom_cmd or cmd_sep.join(config.build(config))
            build_cmd = self.inject_env("cd $BUILD_CWD" + cmd_sep + build_cmd, config)
            vagrant_run_cmd = "vagrant ssh -c '" + build_cmd + "'"

        self.exec_host_cmd(vagrant_run_cmd, config)

    def post_build(self, config):
        if (config.no_shutdown):
            print ("INFO: Vagrant J2V8 machine will continue running...")
            return

        self.exec_host_cmd("vagrant halt", config)
        return
