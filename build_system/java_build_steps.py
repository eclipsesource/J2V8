import shared_build_steps as u

def add_java_step(platform_config, build_step, step_cmds):
    # add the common preparation sequence for a maven build-step to the platform-config
    if not hasattr(platform_config, "prepare_maven"):
        platform_config.prepare_maven = lambda config: \
            u.clearNativeLibs(config) + \
            u.copyNativeLibs(config) + \
            u.setJavaHome(config)
    #-----------------------------------------------------------------------
    # add a build-step that involves running maven and requires some preparation
    def java_build_step(cmds):
        def build_func(config):
            # update maven pom.xml settings
            u.apply_maven_config_settings(config)

            # assemble the commands for this build-step
            # includes the preparation commands for maven
            steps = \
                platform_config.prepare_maven(config) + \
                cmds + \
                u.copyOutput(config)

            # the shell was already prepared for running maven,
            # if another java step will run later on this does not to be done again
            platform_config.prepare_maven = lambda cfg: ["echo Native lib already copied..."]

            return steps
        return build_func
    #-----------------------------------------------------------------------
    platform_config.build_step(build_step, java_build_step(step_cmds))
    #-----------------------------------------------------------------------
