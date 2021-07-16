echo y | \
  if [ "x${target_os}" = "xandroid" ]; then \
    ./build/install-build-deps-android.sh ; \
  else \
    ./build/install-build-deps.sh ; fi

# install the latest gcc/g++
echo 'deb http://deb.debian.org/debian testing main' >> /etc/apt/sources.list
echo "/usr/i686-linux-gnu/lib" >> /etc/ld.so.conf.d/i386-linux-gnu.conf
apt update -y && \
  DEBIAN_FRONTEND=noninteractive apt install -y gcc build-essential lib32stdc++6 libatomic1-i386-cross
ldconfig
