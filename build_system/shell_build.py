import subprocess
import sys
from .build_structures import BuildSystem
from . import constants as c
from . import build_utils as utils

class ShellBuildSystem(BuildSystem):
    def clean(self, config):
        return

    def health_check(self, config):
        try:
            shell_check_cmd = "ver" if utils.is_win32(config.platform) else "bash --version"
            self.exec_cmd(shell_check_cmd, config)
        except subprocess.CalledProcessError:
            utils.cli_exit("ERROR: Failed Shell build-system health check!")

    def pre_build(self, config):
        return

    def exec_build(self, config):
        print ("SHELL building " + config.platform + "@" + config.arch + " => " + config.name)

        build_cmd = config.custom_cmd or " && ".join(config.build(config))
        shell_str = self.inject_env("cd $BUILD_CWD && " + build_cmd, config)

        self.exec_cmd(shell_str, config)

    def post_build(self, config):
        return
