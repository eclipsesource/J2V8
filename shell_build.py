import subprocess
import sys
from cross_build import BuildSystem

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

        build_cmds_str = self.inject_env(custom_cmd or " && ".join(config.build(config, arch)), config, arch)

        self.exec_cmd(build_cmds_str, config, arch)

    def post_build(self, config, arch):
        return
