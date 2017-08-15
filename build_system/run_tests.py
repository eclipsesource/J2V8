"""
This is the main entry-point for running the J2V8 build-system test suite.
Some of the tests require certain environmental conditions to be able to run,
e.g. on Windows, Vagrant (using the Virtual-Box provider) can not be run side-by-side
with HyperV (which is used by Docker-For-Windows virtualization) and therefore it
always requires a reconfiguration of OS-level virtualization features and a reboot
before one or the other collection of tests can be run.

Therefore if you want to run the unit-tests below, you currently have to cherry-pick
the ones that can be run together on your particular host-platform environment & configuration.
"""
from unittest import TestLoader, TestSuite
from tests.runner.test_runner import SurePhyreTestRunner

import tests.test_android_docker
import tests.test_alpine_linux_docker
import tests.test_linux_docker
import tests.test_macos_vagrant
import tests.test_win32_docker
import tests.test_win32_native

# TODO: we could add some clever host-environment detection logic to even
# automate the decision which tests can or can not be run
loader = TestLoader()
suite = TestSuite((
    # loader.loadTestsFromModule(tests.test_android_docker),
    loader.loadTestsFromModule(tests.test_alpine_linux_docker),
    loader.loadTestsFromModule(tests.test_linux_docker),
    # loader.loadTestsFromModule(tests.test_macos_vagrant),
    # loader.loadTestsFromModule(tests.test_win32_docker),
    # loader.loadTestsFromModule(tests.test_win32_native),
))

runner = SurePhyreTestRunner()
runner.run(suite)
