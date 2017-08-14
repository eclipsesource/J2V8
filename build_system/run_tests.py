from unittest import TestLoader, TestSuite
from tests.runner.test_runner import SurePhyreTestRunner

import tests.test_linux_docker
import tests.test_macos_vagrant
import tests.test_win32_docker
import tests.test_win32_native

loader = TestLoader()
suite = TestSuite((
    loader.loadTestsFromModule(tests.test_linux_docker),
    # loader.loadTestsFromModule(tests.test_macos_vagrant),
    # loader.loadTestsFromModule(tests.test_win32_docker),
    # loader.loadTestsFromModule(tests.test_win32_native),
))

runner = SurePhyreTestRunner()
runner.run(suite)
