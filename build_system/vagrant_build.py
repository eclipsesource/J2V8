import subprocess
import sys
from cross_build import BuildSystem

class VagrantBuildSystem(BuildSystem):
    def clean(self, config):
        return

    def health_check(self, config):
        try:
            self.exec_host_cmd("vagrant global-status", config)
        except subprocess.CalledProcessError:
            sys.exit("ERROR: Failed Vagrant build-system health check, make sure Vagrant is available and running!")

    def pre_build(self, config):
        self.exec_host_cmd("vagrant up", config)

    def exec_build(self, config):
        print ("VAGRANT building " + config.platform + "@" + config.arch + " => " + config.name)

        build_cmd = config.custom_cmd or "; ".join(config.build(config))
        platform_cmd = "vagrant ssh -c '" + self.inject_env("cd $BUILD_CWD; " + build_cmd, config) + "'"

        self.exec_host_cmd(platform_cmd, config)

    def post_build(self, config):
        self.exec_host_cmd("vagrant halt", config)
