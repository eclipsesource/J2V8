#!/bin/bash
adb wait-for-device

A=$(adb shell getprop sys.boot_completed | tr -d '\r')

echo "Waiting for emulator to boot (this can take several minutes)"

while [ "$A" != "1" ]; do
        sleep 5
        echo "$(date +%T) waiting for emulator to boot..."
        A=$(adb shell getprop sys.boot_completed | tr -d '\r')
done

adb shell input keyevent 82
