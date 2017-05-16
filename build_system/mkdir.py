import os
import sys

# this is a cross-platform polyfill for "mkdir -p"

directory = sys.argv[1]

if not os.path.exists(directory):
    os.makedirs(directory)
