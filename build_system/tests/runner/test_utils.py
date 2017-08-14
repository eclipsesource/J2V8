
def write_log(level, message = ""):
    print "$ [%(level)s] %(message)s" % locals()

def write_separator():
    print "$---------------------------------------------------------------------------------------------------"

def get_test_names(test):
    test_class = type(test).__name__
    test_method = test._testMethodName
    return (test_class, test_method)
