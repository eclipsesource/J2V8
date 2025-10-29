ARG sys_image=debian:12

FROM $sys_image

# default values
ARG vendor=debian
ARG target_os=linux
ARG target_cpu=x64

# Update dependency packages needed for building V8
RUN apt-get -qq update && \
  DEBIAN_FRONTEND=noninteractive apt-get -qq install -y --no-install-recommends \
    apt-utils \
    ca-certificates \
    curl \
    git \
    gnupg \
    lbzip2 \
    lsb-release \
    pkg-config \
    python3 \
    python3-distutils \
    python3-venv \
    sudo \
    unzip \
    wget \
    xz-utils \
    build-essential \
 && rm -rf /var/lib/apt/lists/*

# Ensure legacy scripts find python
RUN ln -sf /usr/bin/python3 /usr/bin/python

RUN mkdir -p /v8build
WORKDIR /v8build

# DEPOT TOOLS install
RUN git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
ENV PATH /v8build/depot_tools:"$PATH"
RUN echo $PATH

# Fetch V8 code
RUN fetch v8
WORKDIR /v8build/v8
RUN git fetch origin refs/branch-heads/12.4:refs/branch-heads/12.4
RUN git checkout refs/branch-heads/12.4
WORKDIR /v8build

ENV target_os ${target_os}
RUN echo "target_os= ['${target_os}']" >> .gclient
ENV CIPD_CACHE_DIR=/tmp/cipd-cache
ENV CIPD_INSTALL_MODE=copy
ENV CIPD_DISABLE_UPDATE_CHECK=1

# Make sure any CIPD data left from earlier layers is removed
RUN rm -rf /v8build/.cipd
RUN gclient sync -D

WORKDIR /v8build/v8
COPY install-deps.sh .
RUN echo y | sh install-deps.sh

# Install sysroots needed for cross-compilation
# Always install x64 sysroot for host tools
RUN build/linux/sysroot_scripts/install-sysroot.py --arch=amd64

# Install additional sysroots for Android cross-compilation
RUN if [ "${target_os}" = "android" ]; then \
      if [ "${target_cpu}" = "arm" ]; then \
        build/linux/sysroot_scripts/install-sysroot.py --arch=arm; \
      elif [ "${target_cpu}" = "arm64" ]; then \
        build/linux/sysroot_scripts/install-sysroot.py --arch=arm64; \
      elif [ "${target_cpu}" = "x86" ]; then \
        build/linux/sysroot_scripts/install-sysroot.py --arch=i386; \
      fi; \
    fi

ENV target_cpu ${target_cpu}
ENV build_platform ${target_cpu}.release
ENV path_to_args ${target_os}-${target_cpu}/args.gn

# Create output directory and copy custom args.gn
# Skip v8gen.py as it fails with --check flag in V8 12.4 for cross-compilation setups
RUN mkdir -p out.gn/${build_platform}
COPY ./${path_to_args} out.gn/${build_platform}/args.gn

# Display the configuration
RUN ls -al out.gn/${build_platform}/
RUN cat out.gn/${build_platform}/args.gn

# Run GN directly to generate build files
RUN gn gen out.gn/${build_platform}

# Build the V8 monolithic static liblary
RUN ninja -j 4 -C out.gn/${build_platform} -t clean
RUN ninja -j 4 -C out.gn/${build_platform} v8_monolith
