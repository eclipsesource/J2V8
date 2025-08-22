echo "Preparing Debian packages..."

#dpkg --add-architecture i386

apt-get -qq update && \
    DEBIAN_FRONTEND=noninteractive apt-get -qq install -y \
    git \
    unzip \
    gcc \
    g++ \
    curl \
    file \
    python3 \
    python-is-python3 \
    make \
    wget \
    supervisor
