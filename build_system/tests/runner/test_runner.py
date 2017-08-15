import datetime
import os
import sys
from unittest import TestSuite

import __main__

from test_result import TestResult, TestOutcome
import test_utils as utils

class SurePhyreTestRunner(object):
    """ Run the given TestSuite and collect statistics & timing information about the tests being run. """
    def __init__(self):
        self.runner_start_time = None
        self.runner_stop_time = None

    def run(self, test):
        "Run the given test case or test suite."

        self.runner_start_time = datetime.datetime.now()

        test_class_dict = {}

        def find_test_methods(test_decl):
            is_iterable = hasattr(test_decl, '__iter__')

            if (is_iterable):
                for tests in test_decl:
                    find_test_methods(tests)
            else:
                cls_nm = type(test_decl).__name__

                if not test_class_dict.get(cls_nm):
                    test_class_dict[cls_nm] = list()

                test_class_dict[cls_nm].append(test_decl)

        # convert the given TestCase/TestSuite into a dictionary of test-classes
        find_test_methods(test)

        all_results = list()
        success_results = list()
        failure_results = list()
        error_results = list()
        skipped_results = list()

        utils.write_separator()
        utils.write_log("INFO", "T E S T S")

        for k, class_tests in test_class_dict.iteritems():
            class_suite = TestSuite(class_tests)
            reports_dir = os.path.join(os.path.dirname(__main__.__file__), "test-reports")

            if not os.path.exists(reports_dir):
                os.makedirs(reports_dir)

            with file(os.path.join(reports_dir, k + '.txt'), 'wb') as fp:
                # execute all tests in this test class
                class_result = TestResult([sys.stdout, fp], class_tests)
                class_suite(class_result)

                # get the test-results from this class and add them to the summary lists
                all_results.extend(class_result.all_results)
                success_results.extend(class_result.success_results)
                failure_results.extend(class_result.failure_results)
                error_results.extend(class_result.error_results)
                skipped_results.extend(class_result.skipped_results)

        tests_success = not any(error_results) and not any(failure_results)
        tests_result = "SUCCESS" if tests_success else "FAILURE"
        self.runner_stop_time = datetime.datetime.now()

        # print final summary log after all tests are done running
        print
        utils.write_separator()
        utils.write_log("INFO", "TESTS RUN %(tests_result)s" % locals())
        utils.write_separator()
        utils.write_log("INFO")
        utils.write_log("INFO", "Results:")

        if not tests_success:
            utils.write_log("INFO")

        def print_summary_problems(err_list, kind):
            if (any(err_list)):
                utils.write_log("ERROR", kind + "s: ")

                for r in err_list:
                    test_class, test_method = utils.get_test_names(r.test)
                    err_message = r.errObj[1].message
                    err_frame = r.errObj[2].tb_next
                    err_lineno = err_frame.tb_lineno if err_frame else ""
                    utils.write_log("ERROR", "  %(test_class)s.%(test_method)s:%(err_lineno)s %(err_message)s" % locals())

        print_summary_problems(failure_results, "Failure")
        print_summary_problems(error_results, "Error")

        num_success = len(success_results)
        num_failures = len(failure_results)
        num_errors = len(error_results)
        num_skips = len(skipped_results)

        utils.write_log("INFO")
        utils.write_log("ERROR", "Tests run: %(num_success)s, Failures: %(num_failures)s, Errors: %(num_errors)s, Skipped: %(num_skips)s" % locals())
        utils.write_log("INFO")

        total_elapsed = self.runner_stop_time - self.runner_start_time

        utils.write_separator()
        utils.write_log("INFO", "Total time: %(total_elapsed)s s" % locals())
        utils.write_log("INFO", "Finished at: %s" % self.runner_stop_time)
        utils.write_separator()
