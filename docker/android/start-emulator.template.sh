echo no | $ANDROID_HOME/tools/android create avd -f -n test -t android-19 --abi default/$IMG_ARCH
echo no | $ANDROID_HOME/tools/emulator$EMU_ARCH -avd test -noaudio -no-window -gpu off -verbose -qemu -usbdevice tablet -vnc :0
