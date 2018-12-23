echo "Preparing CMake..."
curl https://github.com/Kitware/CMake/releases/download/v3.13.2/cmake-3.13.2-Linux-x86_64.sh -O -L
mkdir /opt/cmake
chmod +x cmake-3.13.2-Linux-x86_64.sh
./cmake-3.13.2-Linux-x86_64.sh --prefix=/opt/cmake --skip-license
rm -rf cmake-3.13.2-Linux-x86_64.sh
