echo no | $ANDROID_HOME/tools/android create avd -f -n test -t android-19 --abi default/$ARCH
echo no | $ANDROID_HOME/tools/emulator64-$ARCH -avd test -noaudio -no-window -gpu off -verbose -qemu -usbdevice tablet -vnc :0
