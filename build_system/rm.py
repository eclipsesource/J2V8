import os
import sys
import shutil

# this is a cross-platform polyfill for "rm"

items = sys.argv[1:]

for item in items:
    if (os.path.isdir(item)):
        shutil.rmtree(item)
    else:
        if(os.path.exists(item)):
            os.remove(item)
