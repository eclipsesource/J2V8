
def expectOutput(regex):
    """ After a test is completed successfully, also verify that the CLI output contains an expected regex pattern. """
    def expectOutput_wrapper(func):
        func.__testRegex = regex
        return func
    return expectOutput_wrapper
