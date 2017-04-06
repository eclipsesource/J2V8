FROM philcryer/min-jessie:latest

WORKDIR /build/

ENV NDK_VERSION r13b

ENV NDK /build/android-ndk-$NDK_VERSION
ENV NDK_NAME android-ndk-$NDK_VERSION-linux-x86_64
ENV PATH $PATH:/build/android-gcc-toolchain:$NDK

ENV CCFLAGS -fPIC
ENV CXXFLAGS -fPIC


RUN echo "[0/3] installing packages..." && \
	apt-get -qq update && \

	DEBIAN_FRONTEND=noninteractive apt-get -qq install -y \
	git \
	unzip \
	gcc gcc-multilib \
	g++ g++-multilib \
	curl \
	file

RUN echo "[1/2] cloning dependencies..." && \
	git clone https://github.com/sjitech/android-gcc-toolchain && \
	git clone https://github.com/nodejs/node -b v7.4.0

RUN echo "[2/2] setting up..." && \
 	curl -fSOL https://dl.google.com/android/repository/$NDK_NAME.zip && \
	unzip $NDK_NAME.zip && \
	rm $NDK_NAME.zip

COPY jni /build/jni/
CMD android-gcc-toolchain arm --api 19 --host gcc-lpthread,gcc-m32 -C \
	sh -c "cd node; ./configure --dest-cpu=arm --dest-os=android --without-snapshot --enable-static && make && cd ../jni && ndk-build"
