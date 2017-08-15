
def expectOutput(regex):
    """ After a test is completed successfully, also verify that the CLI output contains an expected regex pattern. """
    def expectOutput_wrapper(func):

        if not hasattr(func, "__testRegex"):
            func.__testRegex = []

        is_iterable = hasattr(regex, '__iter__')

        if is_iterable:
            for rx in regex:
                func.__testRegex.append(rx)
        else:
            func.__testRegex.append(regex)

        return func
    return expectOutput_wrapper
