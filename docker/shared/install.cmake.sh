
echo "Preparing CMake..."
curl https://cmake.org/files/v3.8/cmake-3.8.1-Linux-x86_64.sh -O
mkdir /opt/cmake
chmod +x cmake-3.8.1-Linux-x86_64.sh
./cmake-3.8.1-Linux-x86_64.sh --prefix=/opt/cmake --skip-license
rm -rf cmake-3.8.1-Linux-x86_64.sh
