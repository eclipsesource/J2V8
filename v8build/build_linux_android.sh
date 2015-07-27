NDK_VERSION="android-ndk-r10e"
V8_VERSION=$1
if [ -z "$V8_VERSION" ]; then
  echo "V8 Version parameter not set!"
  exit 1
fi

function collectLibrary {
  cp v8/out/$1.release/lib.target/libv8.so lib/libv8-$V8_VERSION-$1.so
}

#sudo apt-get update
#sudo apt-get install -q -y git gcc g++ make python binutils tar bzip2 gzip curl wget libc6-dev-i386 g++-multilib

if [ ! -d "depot_tools/" ]; then
  git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
fi
export PATH=`pwd`/depot_tools:"$PATH"

if [ ! -f "$NDK_VERSION-linux-x86_64.bin" ]; then
    wget http://dl.google.com/android/ndk/$NDK_VERSION-linux-x86_64.bin
    chmod +x $NDK_VERSION-linux-x86_64.bin
fi

if [ ! -d "v8/" ]; then
  fetch v8
fi

cd v8
git fetch
git checkout $V8_VERSION
gclient sync

if [ ! -d "third_party/android_tools/ndk/" ]; then
  mkdir -p third_party/android_tools/
  ./../$NDK_VERSION-linux-x86_64.bin -othird_party/android_tools/
  mv third_party/android_tools/$NDK_VERSION/ third_party/android_tools/ndk/
fi

make ia32.release x64.release arm.release arm64.release android_arm.release library=shared i18nsupport=off -j8
# make android_ia32.release library=shared -j8

cd ..
mkdir -p lib/
rm -Rf lib/*
collectLibrary ia32
collectLibrary x64
collectLibrary arm
collectLibrary arm64
collectLibrary android_arm
