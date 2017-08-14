import unittest

from runner.test_asserts import *

import constants as c
import build_executor as bex

class TestLinuxDocker(unittest.TestCase):

    def with_x64_defaults(self, params):
        x64_defaults = {
            "target": c.target_linux,
            "arch": c.arch_x64,
            "docker": True,
            "redirect_stdout": True, # important for test-logging
        }
        params.update(x64_defaults)
        return params

    @expectOutput(r"\[WARNING\] Tests run: 1, Failures: 0, Errors: 0, Skipped: 1, Time elapsed: \d+.\d+ s - in com\.eclipsesource\.v8\.NodeJSTest")
    def test_x64_node_disabled(self):

        params = self.with_x64_defaults(
        {
            "buildsteps": ["j2v8", "test"],
        })

        bex.execute_build(params)

    # TODO: could use functional parameter overload to return error message + details
    # (e.g. match regex groups for numfails, numerrors, numskips, etc. and make advanced asserts)
    @expectOutput(r"\[INFO\] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: \d+.\d+ s - in com\.eclipsesource\.v8\.NodeJSTest")
    def test_x64_node_enabled(self):

        params = self.with_x64_defaults(
        {
            "node_enabled": True,
            "buildsteps": ["j2v8", "test"],
        })

        bex.execute_build(params)
