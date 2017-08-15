import unittest

from runner.test_asserts import *

import constants as c
import build_executor as bex

class TestAndroidDocker(unittest.TestCase):

    def with_x86_defaults(self, params):
        x86_defaults = {
            "target": c.target_android,
            "arch": c.arch_x86,
            "docker": True,
            "redirect_stdout": True, # important for test-logging
        }
        params.update(x86_defaults)
        return params

    @expectOutput([
        r"assumption failure org\.junit\.AssumptionViolatedException: Skipped test \(Node\.js features not included in native library\)",
        r"Total tests 9, assumption_failure 9",
        r"\n:spoon\n\nBUILD SUCCESSFUL\n\nTotal time: ",
    ])
    def test_x86_node_disabled(self):

        params = self.with_x86_defaults(
        {
            "buildsteps": ["j2v8", "test"],
            "j2v8test": "-PtestClass=com.eclipsesource.v8.NodeJSTest",
        })

        bex.execute_build(params)

    @expectOutput(r"\n:spoon\n\nBUILD SUCCESSFUL\n\nTotal time: ")
    def test_x86_node_enabled(self):

        params = self.with_x86_defaults(
        {
            "node_enabled": True,
            "buildsteps": ["j2v8", "test"],
            "j2v8test": "-PtestClass=com.eclipsesource.v8.NodeJSTest",
        })

        bex.execute_build(params)
