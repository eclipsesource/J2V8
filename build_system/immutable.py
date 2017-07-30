# for original source see: http://code.activestate.com/recipes/576527-freeze-make-any-object-immutable/

immutable_types = set((int, str, bool))

class Frozen(object):
    def __init__(self, value):
        self._value = value

    def __getattribute__(self, name):
        if name == '_value': return super(Frozen, self).__getattribute__(name)
        v = getattr(self._value, name)
        if v is None or callable(v):
            return v
        return v if v.__class__ in immutable_types else freeze(v)

    def __setattr__(self, name, value):
        if name == '_value': super(Frozen, self).__setattr__(name, value)
        else: raise Exception("Can't modify frozen object {0}".format(self._value))

def freeze(value):
    return Frozen(value)
