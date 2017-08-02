"""
This file contains the collection of build-configurations that are available
for selection when running the build.py script with the --interactive, -i parameter.

Parameters for the build can be specified by their variable-name ("dest" defined in the cli.py arguments).
An array of build-steps can also be specified here, if none are specified then "all" steps will be run.
"""
import constants as c

configs = [
      # ANDROID builds
      {
            "name": "Docker >> android-x86 >> NODE_ENABLED",
            "params": {
                  "target": c.target_android,
                  "arch": c.arch_x86,
                  "docker": True,
                  "node_enabled": True,
            },
      },
      {
            "name": "Docker >> android-arm >> NODE_ENABLED",
            "params": {
                  "target": c.target_android,
                  "arch": c.arch_arm,
                  "docker": True,
                  "node_enabled": True,
            },
      },
      # LINUX builds
      {
            "name": "Docker >> alpine-linux-x64 >> NODE_ENABLED",
            "params": {
                  "target": c.target_linux,
                  "vendor": "alpine",
                  "arch": c.arch_x64,
                  "docker": True,
                  "sys_image": "openjdk:8u131-alpine",
                  "node_enabled": True,
            },
      },
      # TODO: build not supported, because default gcc/g++ on alpine does not support x32 compilation
      # (see: https://stackoverflow.com/a/40574830/425532)
      # {
      #       "name": "Docker >> alpine-linux-x86 >> NODE_ENABLED",
      #       "params": {
      #             "target": c.target_linux,
      #             "vendor": "alpine",
      #             "arch": c.arch_x86,
      #             "docker": True,
      #             "sys_image": "openjdk:8u131-alpine",
      #             "node_enabled": True,
      #       },
      # },
      {
            "name": "Docker >> linux-x64 >> NODE_ENABLED",
            "params": {
                  "target": c.target_linux,
                  "arch": c.arch_x64,
                  "docker": True,
                  "node_enabled": True,
            },
      },
      {
            "name": "Docker >> linux-x86 >> NODE_ENABLED",
            "params": {
                  "target": c.target_linux,
                  "arch": c.arch_x86,
                  "docker": True,
                  "node_enabled": True,
            },
      },
      # MACOSX builds
      {
            "name": "Vagrant >> macosx-x64 >> NODE_ENABLED",
            "params": {
                  "target": c.target_macos,
                  "arch": c.arch_x64,
                  "vagrant": True,
                  "node_enabled": True,
            },
      },
      {
            "name": "Vagrant >> macosx-x86 >> NODE_ENABLED",
            "params": {
                  "target": c.target_macos,
                  "arch": c.arch_x86,
                  "vagrant": True,
                  "node_enabled": True,
            },
      },
      # WINDOWS builds
      {
            "name": "Native >> windows-x64 >> NODE_ENABLED",
            "params": {
                  "target": c.target_win32,
                  "arch": c.arch_x64,
                  "node_enabled": True,
            },
      },
      # TODO: this build is currently broken due to a Node.js build-system issue
      # {
      #       # see: https://github.com/nodejs/node/issues/13569
      #       "name": "Native >> windows-x86 >> NODE_ENABLED",
      #       "params": {
      #             "target": c.target_win32,
      #             "arch": c.arch_x86,
      #             "node_enabled": True,
      #       },
      # },
      {
            "name": "Docker >> windows-x64 >> NODE_ENABLED",
            "params": {
                  "target": c.target_win32,
                  "arch": c.arch_x64,
                  "docker": True,
                  "node_enabled": True,
            },
      },
      {
            "name": "Vagrant >> windows-x64 >> NODE_ENABLED",
            "params": {
                  "target": c.target_win32,
                  "arch": c.arch_x64,
                  "vagrant": True,
                  "node_enabled": True,
            },
      },
]
