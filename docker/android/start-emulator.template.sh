echo no | $ANDROID_HOME/tools/android create avd -f -n test -k "system-images;android-19;default;armeabi-v7a"
echo no | $ANDROID_HOME/emulator/emulator64$EMU_ARCH -avd test -noaudio -no-window -gpu off -verbose -qemu -usbdevice tablet -vnc :0
