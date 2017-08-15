import collections
import datetime
import re
import StringIO
import sys
import traceback
import unittest

from output_redirector import OutputRedirector
import test_utils as utils

TestResultBase = unittest.TestResult

class TestOutcome:
    Success, Failure, Error, Skip = range(4)

__TestRunData = collections.namedtuple("TestRunData", "outcome test errStr errObj output elapsed")
__TestRunData.__new__.__defaults__ = (None,) * len(__TestRunData._fields)

class TestRunData(__TestRunData):
    """
    Immutable tuple data-structure that contains the results of a single test-method that has been run.

    outcome -> one of the enumeration values of the "TestOutcome" class (Success, Failure, Error, Skip)
    test -> information about which test-method this data is about
    errStr -> an error string that was emitted if this test was not successful
    errObj -> details about the output, exception and stackframe that were involved in a failing test
    output -> a plain-text string of all the output (stdout/stdout) that was generated during this test
    elapsed -> the duration that it took for the test-method to run
    """
    pass

class TestResult(TestResultBase):
    """
    Collects and processes the results from an invoked set of tests.
    
    The main purpose is to:
    1) Track times that individual tests needed to complete
    2) Collect the stdout/stderr that each of the tests produces
    3) Collect statistics and reporting-details for successful and failed test-runs
    """
    def __init__(self, streams, test_cases):
        TestResultBase.__init__(self)
        self.__sys_stdout = None
        self.__sys_stderr = None

        self.streams = streams
        self.test_cases = test_cases

        self.class_start_time = None
        self.class_stop_time = None

        self.test_start_time = None
        self.test_stop_time = None

        # list of all generated TestRunData for this result
        self.all_results = []

        # lists of type-specific TestRunData for this result
        self.success_results = []
        self.failure_results = []
        self.error_results = []
        self.skipped_results = []

    #override
    def startTest(self, test):
        TestResultBase.startTest(self, test)

        # remember the original sys streams
        self.__sys_stdout = sys.stdout
        self.__sys_stderr = sys.stderr

        # just one buffer for both stdout and stderr
        self.outputBuffer = StringIO.StringIO()

        sys.stdout = OutputRedirector(self.streams + [self.outputBuffer])
        sys.stderr = OutputRedirector(self.streams + [self.outputBuffer])

        # now the real testing logic kicks in
        test_class, test_method = utils.get_test_names(test)

        if (not self.class_start_time):
            self.class_start_time = datetime.datetime.now()

        self.test_start_time = datetime.datetime.now()

        utils.write_separator()
        utils.write_log("INFO", "Running %(test_class)s.%(test_method)s" % locals())

    def finish_test(self, test):
        """
        This is run after each single test-method is finished, but the below logic
        will only be executed once the very last test-method from the original
        set of given unit-tests is completed.
        """
        if (self.testsRun != len(self.test_cases)):
            return

        if (not self.class_stop_time):
            self.class_stop_time = datetime.datetime.now()

        num_tests = len(self.all_results)
        num_failures = len(self.failure_results)
        num_errors = len(self.error_results)
        num_skips = len(self.skipped_results)

        test_class, _ = utils.get_test_names(test)

        test_elapsed = self.class_stop_time - self.class_start_time

        log_level = "INFO"
        failure_tag = ""

        if (num_failures or num_errors):
            log_level = "ERROR"
            failure_tag = "<<< FAILURE! "
        elif (num_skips):
            log_level = "WARNING"
        
        utils.write_separator()
        print
        utils.write_separator()
        utils.write_log(log_level, "Tests run: %(num_tests)s, Failures: %(num_failures)s, Errors: %(num_errors)s, Skipped: %(num_skips)s, Time elapsed: %(test_elapsed)s s %(failure_tag)s- in %(test_class)s" % locals())
        utils.write_separator()

        def print_errors(test_class, err_list, kind):
            for result in err_list:
                test = result.test
                elapsed = result.elapsed
                test_method = test._testMethodName
                utils.write_log("ERROR", "%(test_method)s(%(test_class)s)  Time elapsed: %(elapsed)s s  <<< %(kind)s!" % locals())
                err_frame = result.errObj[2].tb_next
                traceback.print_tb(err_frame, 1)
                print

        # write leading newline if detail error reports should be written
        if any(self.error_results) or any(self.failure_results):
            print

        print_errors(test_class, self.error_results, "ERROR")
        print_errors(test_class, self.failure_results, "FAILURE")

    def complete_test_case(self, test, test_info = None):
        """
        Disconnect output redirection and return buffer.
        Safe to call multiple times.
        """
        if (test_info):
            self.test_stop_time = datetime.datetime.now()

            test_output = self.outputBuffer.getvalue()
            test_duration = self.test_stop_time - self.test_start_time

            # merge data produced during test with additional meta-data
            test_result = TestRunData(*(test_info[:-2] + (test_output, test_duration)))

            self.all_results.append(test_result)

            if (test_result.outcome == TestOutcome.Success):
                self.success_results.append(test_result)

            elif (test_result.outcome == TestOutcome.Error):
                self.error_results.append(test_result)

            elif (test_result.outcome == TestOutcome.Failure):
                self.failure_results.append(test_result)

            elif (test_result.outcome == TestOutcome.Skip):
                self.skipped_results.append(test_result)

        if self.__sys_stdout:
            self.finish_test(test)

            # turn off the shell output redirection
            sys.stdout = self.__sys_stdout
            sys.stderr = self.__sys_stderr

            self.__sys_stdout = None
            self.__sys_stderr = None

    #override
    def stopTest(self, test):
        # Usually one of addSuccess, addError or addFailure would have been called.
        # But there are some path in unittest that would bypass this.
        # We must disconnect stdout in stopTest(), which is guaranteed to be called.
        self.complete_test_case(test)

    def __assertTestOutput(self, test):
        test_method = type(test).__dict__.get(test._testMethodName)
        test_regex_field = "__testRegex"

        if (hasattr(test_method, test_regex_field)):
            regex = test_method.__dict__.get(test_regex_field)
            output = self.outputBuffer.getvalue()

            regex_mismatches = []

            for rx in regex:
                match_ok = re.search(rx, output)

                if (not match_ok):
                    regex_mismatches.append(rx)

            if (any(regex_mismatches)):
                mismatches_str = "\n\t\t".join(regex_mismatches)
                try:
                    raise Exception("Unable to find expected patterns in test-output:\n\t\t" + mismatches_str)
                except Exception:
                    ex_nfo = sys.exc_info()
                    self.addFailure(test, ex_nfo)
                return False

        return True

    #override
    def addSuccess(self, test):

        # after a test was successful, also run stdout/stderr asserts
        # which can still result in a test-failure
        if not self.__assertTestOutput(test):
            return

        TestResultBase.addSuccess(self, test)
        testData = TestRunData(TestOutcome.Success, test, '', None)
        self.complete_test_case(test, testData)

    #override
    def addError(self, test, err):
        TestResultBase.addError(self, test, err)
        _, _exc_str = self.errors[-1]
        testData = TestRunData(TestOutcome.Error, test, _exc_str, err)
        self.complete_test_case(test, testData)

    #override
    def addFailure(self, test, err):
        TestResultBase.addFailure(self, test, err)
        _, _exc_str = self.failures[-1]
        testData = TestRunData(TestOutcome.Failure, test, _exc_str, err)
        self.complete_test_case(test, testData)

    #override
    def addSkip(self, test, reason):
        TestResultBase.addSkip(self, test, reason)
        testData = TestRunData(TestOutcome.Skip, test, reason, None)
        self.complete_test_case(test, testData)
