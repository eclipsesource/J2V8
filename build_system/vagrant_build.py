import subprocess
import sys
from cross_build import BuildSystem

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
        print ("VAGRANT building " + config.platform + "@" + arch + " => " + config.name)

        # TODO
        build_cmds_str = self.inject_env("cd $BUILD_CWD; " + (custom_cmd or "; ".join(config.build(config, arch))), config, arch)

        self.exec_host_cmd("vagrant ssh -c '" + build_cmds_str + "'", config, arch)

    def post_build(self, config, arch):
        self.exec_host_cmd("vagrant halt", config, arch)
