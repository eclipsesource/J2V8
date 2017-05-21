FROM philcryer/min-jessie:latest

ENV NDK_VERSION r13b

ENV NDK=/build/android-ndk-$NDK_VERSION \
	NDK_NAME=android-ndk-$NDK_VERSION-linux-x86_64

ENV PATH=$PATH:/build/android-gcc-toolchain:$NDK
ENV PATH=$PATH:/opt/cmake/bin

ENV CCFLAGS=-fPIC CXXFLAGS=-fPIC

RUN echo "Preparing Debian packages..." && \
	apt-get -qq update && \
	DEBIAN_FRONTEND=noninteractive apt-get -qq install -y \
	git \
	unzip \
	gcc gcc-multilib \
	g++ g++-multilib \
	curl \
	file \
	python \
	make

RUN echo "Preparing Android GCC-Toolchain..." && \
	mkdir -p /build && \
	cd build && \
	git clone https://github.com/sjitech/android-gcc-toolchain 

RUN echo "Preparing Android NDK..." && \
	mkdir -p /build && \
	cd build && \
 	curl -fSOL https://dl.google.com/android/repository/$NDK_NAME.zip && \
	unzip $NDK_NAME.zip && \
	rm $NDK_NAME.zip

# RUN DEBIAN_FRONTEND=noninteractive apt-get -qq install -y cmake

RUN echo "Preparing CMake..." && \
	curl https://cmake.org/files/v3.8/cmake-3.8.1-Linux-x86_64.sh -O && \
	mkdir /opt/cmake && \
	chmod +x cmake-3.8.1-Linux-x86_64.sh && \
	./cmake-3.8.1-Linux-x86_64.sh --prefix=/opt/cmake --skip-license && \
	#echo 'export PATH=/opt/cmake/bin:$PATH' >> /etc/environment && \
	rm -rf cmake-3.8.1-Linux-x86_64.sh
