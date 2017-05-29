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

RUN echo "Preparing JDK..." && \
	curl -L -C - -b "oraclelicense=accept-securebackup-cookie" -O http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-linux-x64.tar.gz && \
	mkdir -p /opt/jdk && \
	tar x -C /opt/jdk -f jdk-8u131-linux-x64.tar.gz && \
	update-alternatives --install /usr/bin/java java /opt/jdk/jdk1.8.0_131/bin/java 100 && \
	update-alternatives --install /usr/bin/javac javac /opt/jdk/jdk1.8.0_131/bin/javac 100

RUN echo "Preparing Gradle..." && \
	cd /opt/ && \
	curl -L -O https://services.gradle.org/distributions/gradle-3.5-bin.zip && \
	unzip gradle-3.5-bin.zip
	
ENV GRADLE_HOME=/opt/gradle-3.5
ENV PATH=$PATH:$GRADLE_HOME/bin

RUN echo "Preparing Android SDK..." && \
	mkdir -p /opt/android-sdk-linux && \
	cd /opt/android-sdk-linux && \
	curl -L -O https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip && \
	unzip sdk-tools-linux-3859397.zip

ENV ANDROID_HOME=/opt/android-sdk-linux

RUN yes | /opt/android-sdk-linux/tools/bin/sdkmanager --licenses

# RUN /opt/android-sdk-linux/tools/bin/sdkmanager --update --verbose
RUN /opt/android-sdk-linux/tools/bin/sdkmanager --verbose "build-tools;24.0.3" "platforms;android-10"

RUN mkdir -p /temp

# download the most critical gradle dependencies for the build beforehand
COPY ./shared/build.gradle /temp
WORKDIR /temp
# RUN export MVN_PLATFORM_NAME=temp && \
#     export MVN_ARCH_NAME=temp && \
#     mvn verify -DskipTests || true
RUN cd /temp && gradle || true

