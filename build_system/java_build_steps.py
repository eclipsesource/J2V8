from . import constants as c
from . import shared_build_steps as u

def add_java_build_step(platform_config):
    # after the maven build is complete, copy the JAR artifact to the central output directory
    __add_maven_step(platform_config, c.build_j2v8_java, u.java_build_cmd, [u.copyOutput])

def add_java_test_step(platform_config):
    # running maven tests by themselves usually does not generate any output we need to copy
    __add_maven_step(platform_config, c.build_j2v8_test, u.java_tests_cmd)

def __add_maven_step(platform_config, build_step, step_cmd, post_step_cmds = []):
    # add the common preparation sequence for a maven build-step to the platform-config
    if not hasattr(platform_config, "prepare_maven"):
        platform_config.prepare_maven = lambda config: \
            u.clearNativeLibs(config) + \
            u.copyNativeLibs(config) + \
            u.setJavaHome(config)
    #-----------------------------------------------------------------------
    # add a build-step that involves running maven and requires some preparation
    def java_build_step():
        def build_func(config):
            # update maven pom.xml settings
            u.apply_maven_config_settings(config)

            # add the extra step arguments to the command if we got some
            step_args = getattr(config, "args", None)
            step_args = " " + step_args if step_args else ""

            post_cmds = []

            # post-cmds can be strings or functions
            for ps_cmd in post_step_cmds:
                if callable(ps_cmd):
                    ps = ps_cmd(config)
                    post_cmds += ps
                else:
                    post_cmds.append(ps_cmd)

            # assemble the commands for this build-step
            # includes the preparation commands for maven
            # and also any commands that should be run after the maven command is finished
            steps = \
                platform_config.prepare_maven(config) + \
                [step_cmd + step_args] + \
                post_cmds

            # the shell was already prepared for running maven,
            # if another java step will run later on this does not to be done again
            platform_config.prepare_maven = lambda cfg: ["echo Native lib already copied..."]

            return steps
        return build_func
    #-----------------------------------------------------------------------
    platform_config.build_step(build_step, java_build_step())
    #-----------------------------------------------------------------------
