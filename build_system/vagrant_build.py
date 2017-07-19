import atexit
import subprocess
import sys
import build_utils as utils
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
        vagrant_start_cmd = "vagrant up"

        if (config.pre_build_cmd):
            vagrant_start_cmd = config.pre_build_cmd + utils.host_cmd_sep() + vagrant_start_cmd

        def cli_exit_event():
            if (config.no_shutdown):
                return

            print "Waiting for vagrant virtual-machine to exit..."
            self.exec_host_cmd("vagrant halt", config)

        atexit.register(cli_exit_event)

        self.exec_host_cmd(vagrant_start_cmd, config)

    def exec_build(self, config):
        print ("VAGRANT running " + config.platform + "@" + config.arch + " => " + config.name)

        # shell = "powershell -c \"cmd /C " if utils.is_win32(config.platform) else "ssh -c "
        # cmd_sep = "&& " if utils.is_win32(config.platform) else "; "
        vagrant_run_cmd = None

        if (utils.is_win32(config.platform)):
            # cmd_sep = "\n"
            cmd_sep = "; "
            # cmd_sep = "&& "
            build_cmd = config.custom_cmd or cmd_sep.join(config.build(config))
            # V1
            build_cmd = self.inject_env("cd $BUILD_CWD" + cmd_sep + build_cmd, config)

            # host_cmd_file = self.inject_env("$HOST_CWD/cmd_temp.bat", config)
            # agent_cmd_file = self.inject_env("$BUILD_CWD/vagrant/win32/cmd_temp.bat", config)

            # with open(host_cmd_file, 'w') as f:
            #     f.write(build_cmd)

            # vagrant_run_cmd = "vagrant powershell -c \"cmd /C " + agent_cmd_file + "\""
            # vagrant_run_cmd = "vagrant powershell -c \"Start-Process cmd.exe -RedirectStandardOutput -NoNewWindow -Wait -ArgumentList @('/C', '" + agent_cmd_file + "')\""

            # NOTE: working, just the exit code seems off
            # vagrant_run_cmd = "vagrant powershell -c \"cmd /C " + agent_cmd_file + " | Out-Host\""
            # vagrant_run_cmd = "vagrant powershell -c \"cmd /C " + agent_cmd_file + "\""

            # V1
            vagrant_run_cmd = "vagrant powershell -c \"Invoke-Command { " + build_cmd + " } -ErrorAction Stop\""
            # vagrant_run_cmd = "vagrant powershell -c \"Set-Location -Path $BUILD_CWD" + cmd_sep + "Invoke-Command -ScriptBlock {" + build_cmd + "} -ErrorAction Stop | Select-Object value\""
            # vagrant_run_cmd = self.inject_env(vagrant_run_cmd, config)

            # vagrant_run_cmd = "vagrant powershell -c \"Invoke-Command { " + agent_cmd_file + " } -NoNewScope -ErrorAction Stop\""
            print "run: " + vagrant_run_cmd
        else:
            cmd_sep = "; "
            build_cmd = config.custom_cmd or cmd_sep.join(config.build(config))
            build_cmd = self.inject_env("cd $BUILD_CWD" + cmd_sep + build_cmd, config)
            vagrant_run_cmd = "vagrant ssh -c '" + build_cmd + "'"

        self.exec_host_cmd(vagrant_run_cmd, config)

    def post_build(self, config):
        if (config.no_shutdown):
            return

        self.exec_host_cmd("vagrant halt", config)
        return
