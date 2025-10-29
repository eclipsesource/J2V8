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

ENV target_cpu ${target_cpu}
ENV build_platform ${target_cpu}.release
ENV path_to_args ${target_os}-${target_cpu}/args.gn

RUN ./tools/dev/v8gen.py ${build_platform} -vv

RUN rm out.gn/${build_platform}/args.gn
COPY ./${path_to_args} out.gn/${build_platform}/args.gn
RUN ls -al out.gn/${build_platform}/
RUN cat out.gn/${build_platform}/args.gn
RUN touch out.gn/${build_platform}/args.gn

# Build the V8 monolithic static liblary
RUN ninja -j 4 -C out.gn/${build_platform} -t clean
RUN ninja -j 4 -C out.gn/${build_platform} v8_monolith
