"""
This file contains the collection of build-configurations that are available
for selection when running the build.py script with the --interactive, -i parameter.

Parameters for the build can be specified by their variable-name ("dest" defined in the cli.py arguments).
An array of build-steps can also be specified here, if none are specified then "all" steps will be run.
"""
from . import constants as c

configs = [
      # ANDROID builds
      {
            "name": "android-x86 @ Docker",
            "params": {
                  "target": c.target_android,
                  "arch": c.arch_x86,
                  "docker": True,
                  "node_enabled": True,
            },
      },
      {
            "name": "android-arm @ Docker",
            "params": {
                  "target": c.target_android,
                  "arch": c.arch_arm,
                  "docker": True,
                  "node_enabled": True,
            },
      },
      # ALPINE LINUX builds
      {
            "name": "alpine-linux-x64 @ Docker",
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
      #       "name": "alpine-linux-x86 @ Docker",
      #       "params": {
      #             "target": c.target_linux,
      #             "vendor": "alpine",
      #             "arch": c.arch_x86,
      #             "docker": True,
      #             "sys_image": "openjdk:8u131-alpine",
      #             "node_enabled": True,
      #       },
      # },
      # DEBIAN / UBUNTU LINUX builds
      {
            "name": "linux-x64",
            "params": {
                  "target": c.target_linux,
                  "arch": c.arch_x64,
                  "node_enabled": True,
            },
      },
      {
            "name": "linux-x64 @ Docker",
            "params": {
                  "target": c.target_linux,
                  "arch": c.arch_x64,
                  "docker": True,
                  "node_enabled": True,
            },
      },
      {
            "name": "linux-x86 @ Docker",
            "params": {
                  "target": c.target_linux,
                  "arch": c.arch_x86,
                  "docker": True,
                  "node_enabled": True,
            },
      },
      # MACOSX builds
      {
            "name": "macosx-x64",
            "params": {
                  "target": c.target_macos,
                  "arch": c.arch_x64,
                  "node_enabled": True,
            },
      },
      {
            "name": "macosx-x64 @ Vagrant",
            "params": {
                  "target": c.target_macos,
                  "arch": c.arch_x64,
                  "vagrant": True,
                  "node_enabled": True,
            },
      },
      {
            "name": "macosx-x86 @ Vagrant",
            "params": {
                  "target": c.target_macos,
                  "arch": c.arch_x86,
                  "vagrant": True,
                  "node_enabled": True,
            },
      },
      # WINDOWS builds
      {
            "name": "windows-x64",
            "params": {
                  "target": c.target_win32,
                  "arch": c.arch_x64,
                  "node_enabled": True,
            },
      },
      # TODO: this build is currently broken due to a Node.js build-system issue
      # {
      #       # see: https://github.com/nodejs/node/issues/13569
      #       "name": "windows-x86",
      #       "params": {
      #             "target": c.target_win32,
      #             "arch": c.arch_x86,
      #             "node_enabled": True,
      #       },
      # },
      {
            "name": "windows-x64 @ Docker",
            "params": {
                  "target": c.target_win32,
                  "arch": c.arch_x64,
                  "docker": True,
                  "node_enabled": True,
            },
      },
      {
            "name": "windows-x64 @ Vagrant",
            "params": {
                  "target": c.target_win32,
                  "arch": c.arch_x64,
                  "vagrant": True,
                  "node_enabled": True,
            },
      },
]
