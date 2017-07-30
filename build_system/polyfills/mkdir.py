"""
This is a basic cross-platform polyfill for the "mkdir -p" shell command
"""
import os
import sys

directory = sys.argv[1]

if not os.path.exists(directory):
    os.makedirs(directory)
