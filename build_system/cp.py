import os
import sys
from shutil import copy2

# this is a cross-platform polyfill for "cp"

src = sys.argv[1]
dst = sys.argv[2]

copy2(src, dst)
