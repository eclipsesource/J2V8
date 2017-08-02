"""Provides a simple interactive CLI to start a selected build from a given set of build-configurations"""
import sys

import build_configs as bcfg
import build_executor as bex

def run_interactive_cli():
      idx = 0
      for cfg in bcfg.configs:
            print ("[" + str(idx) + "] " + cfg.get("name"))
            idx += 1
      print # newline

      # NOTE: argv[1] usually should be -i, therefore we need to consider this arg in all checks
      base_arg_count = 2

      sel_index = \
            int(sys.argv[base_arg_count]) \
            if len(sys.argv) > base_arg_count \
            else input("Select a predefined build-configuration to run: ")

      if not isinstance(sel_index, int) or sel_index < 0 or sel_index > len(bcfg.configs):
            sys.exit("ERROR: Must enter a valid test index in the range [0 ... " + str(len(bcfg.configs)) + "]")

      sel_cfg = bcfg.configs[sel_index]

      print ("Building: " + sel_cfg.get("name"))
      print # newline

      build_params = sel_cfg.get("params")

      build_steps = \
            sys.argv[base_arg_count + 1:] \
            if len(sys.argv) > base_arg_count + 1 \
            else raw_input("Override build-steps ? (leave empty to run pre-configured steps): ").split()

      if (len(build_steps) > 0):
            build_params["buildsteps"] = build_steps

      bex.execute_build(build_params)
