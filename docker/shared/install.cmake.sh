echo "Preparing CMake..."

# Detect architecture
ARCH=$(uname -m)
CMAKE_VERSION="3.27.7"

if [ "$ARCH" = "aarch64" ]; then
    CMAKE_ARCH="linux-aarch64"
elif [ "$ARCH" = "x86_64" ]; then
    CMAKE_ARCH="linux-x86_64"
else
    echo "Unsupported architecture: $ARCH"
    exit 1
fi

CMAKE_FILE="cmake-${CMAKE_VERSION}-${CMAKE_ARCH}.sh"
CMAKE_URL="https://github.com/Kitware/CMake/releases/download/v${CMAKE_VERSION}/${CMAKE_FILE}"

echo "Downloading CMake ${CMAKE_VERSION} for ${ARCH}..."
curl -L -O "${CMAKE_URL}"

mkdir -p /opt/cmake
chmod +x "${CMAKE_FILE}"
./"${CMAKE_FILE}" --prefix=/opt/cmake --skip-license
rm -rf "${CMAKE_FILE}"

echo "CMake installation complete"
