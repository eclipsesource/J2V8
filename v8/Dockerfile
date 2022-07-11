ARG sys_image=ubuntu:20.04

FROM $sys_image

# default values
ARG vendor=debian
ARG target_os=linux
ARG target_cpu=x64

# Update depedency of V8
RUN apt-get -qq update && \
    DEBIAN_FRONTEND=noninteractive apt-get install --upgrade -qq -y --no-install-recommends \
    git \
    curl \
    wget \
    build-essential \
    software-properties-common \
    patchelf \
    sudo \
    zip \
    unzip \
    gcc gcc-multilib \
    g++ g++-multilib \
    execstack \
    python3 \
    python \
    python3-pip \
    python3-distutils \
    python3-testresources

RUN mkdir -p /v8build
WORKDIR /v8build

# DEPOT TOOLS install
RUN git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
ENV PATH /v8build/depot_tools:"$PATH"
RUN echo $PATH

# Fetch V8 code
RUN fetch v8
WORKDIR /v8build/v8
RUN git checkout 10.3.174.14
RUN sed -i 's/snapcraft/nosnapcraft/g' ./build/install-build-deps.sh
RUN ./build/install-build-deps.sh
RUN sed -i 's/nosnapcraft/snapcraft/g' ./build/install-build-deps.sh
WORKDIR /v8build

ENV target_os ${target_os}
RUN echo "target_os= ['${target_os}']" >> .gclient
RUN gclient sync

ENV target_cpu ${target_cpu}
ENV build_platform ${target_cpu}.release
ENV path_to_args ${target_os}-${target_cpu}/args.gn

WORKDIR /v8build/v8
RUN ./tools/dev/v8gen.py ${build_platform} -vv

RUN rm out.gn/${build_platform}/args.gn
COPY ./${path_to_args} out.gn/${build_platform}/args.gn
RUN ls -al out.gn/${build_platform}/
RUN cat out.gn/${build_platform}/args.gn
RUN touch out.gn/${build_platform}/args.gn

# Build the V8 monolithic static liblary
RUN ninja -C out.gn/${build_platform} -t clean
RUN ninja -C out.gn/${build_platform} v8_monolith
