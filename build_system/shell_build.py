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
            # TODO: does bash work for MacOS ?
            # TODO: add Win32 CMD build-system to avoid dependency on bash or add a platform switch-case here
            self.exec_cmd("bash --version", config, arch)
        except subprocess.CalledProcessError:
            sys.exit("ERROR: Failed Shell build-system health check!")

    def pre_build(self, config, arch):
        return

    def exec_build(self, config, arch, custom_cmd):
        print ("SHELL building " + config.platform + "@" + arch + " => " + config.name)

        build_cmds_str = self.inject_env("cd $BUILD_CWD && " + (custom_cmd or " && ".join(config.build(config, arch))), config, arch)

        self.exec_cmd(build_cmds_str, config, arch)

    def post_build(self, config, arch):
        return
