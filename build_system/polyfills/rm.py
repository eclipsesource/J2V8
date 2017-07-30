"""
This is a basic cross-platform polyfill for the "rm" shell command
"""
import os
import sys
import shutil

items = sys.argv[1:]

for item in items:
    if (os.path.isdir(item)):
        shutil.rmtree(item)
    else:
        if(os.path.exists(item)):
            os.remove(item)
