FROM ubuntu:latest
MAINTAINER EclipseSource <admin@eclipsesource.com>

# update and upgrade
RUN apt-get update && apt-get upgrade -y && apt-get clean
# install standard toolset
RUN apt-get install -q -y git subversion make gcc bzip2 python g++ libc6-dev-i386 g++-multilib openjdk-7-jdk

ADD http://dl.google.com/android/ndk/android-ndk-r10d-linux-x86_64.bin ./
RUN chmod a+x android-ndk-r10d-linux-x86_64.bin
RUN ./android-ndk-r10d-linux-x86_64.bin
ENV ANDROID_NDK_ROOT /android-ndk-r10d

# Checkout
RUN mkdir /data
RUN cd /data && svn checkout http://v8.googlecode.com/svn/branches/3.26 v8_3_26
ENV CCFLAGS -fPIC
ENV CXXFLAGS -fPIC

# Build
RUN cd /data/v8_3_26 && make dependencies
RUN cd /data/v8_3_26 && make native i18nsupport=off
RUN cd /data/v8_3_26 && make android_arm.release i18nsupport=off
RUN cd /data/v8_3_26 && make android_ia32.release i18nsupport=off
