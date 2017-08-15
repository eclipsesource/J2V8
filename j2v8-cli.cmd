:: This script adds aliases for some of the most often used commands for building J2V8
:: to your current command-shell instance. (can be invoked as "j2v8-cli")
@echo off

doskey build=python build.py $*
doskey nodejs=python nodejs.py $*
doskey citests=python build_system\run_tests.py $*
