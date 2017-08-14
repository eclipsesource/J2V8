@echo off

doskey build=python build.py $*
doskey nodejs=python nodejs.py $*
doskey citests=python build_system\run_tests.py $*
