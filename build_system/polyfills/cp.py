"""
This is a basic cross-platform polyfill for the "cp" shell command
"""
import os
import sys
from shutil import copy2

src = sys.argv[1]
dst = sys.argv[2]

copy2(src, dst)
