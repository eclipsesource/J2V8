
class OutputRedirector(object):
    """ Wrapper to redirect stdout, stderr or any other stream that it is given """
    def __init__(self, streams):
        self.streams = streams

    def write(self, data):
        for s in self.streams:
            s.write(data)

    def writelines(self, lines):
        for s in self.streams:
            s.writelines(lines)

    def flush(self):
        for s in self.streams:
            s.flush()
