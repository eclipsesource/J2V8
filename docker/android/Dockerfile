# FROM philcryer/min-jessie:latest
FROM ubuntu:16.04

ENV NDK_VERSION r13b

ENV NDK=/build/android-ndk-$NDK_VERSION \
	NDK_NAME=android-ndk-$NDK_VERSION-linux-x86_64

ENV PATH=$PATH:/build/android-gcc-toolchain:$NDK
ENV PATH=$PATH:/opt/cmake/bin

# TODO: move to actual build command ?!
ENV CCFLAGS=-fPIC CXXFLAGS=-fPIC

# TODO: call into shared scripts for installing common build dependencies instead
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

# V1 --------------------------------------------------

# ENV ANDROID_HOME=/opt/android-sdk
# ENV PATH $PATH:$ANDROID_HOME/tools
# ENV PATH $PATH:$ANDROID_HOME/platform-tools

# RUN echo "Preparing Android SDK..." && \
# 	mkdir -p $ANDROID_HOME && \
# 	cd $ANDROID_HOME && \
# 	curl -L -O https://dl.google.com/android/repository/sdk-tools-linux-3859397.zip && \
# 	unzip sdk-tools-linux-3859397.zip && \
# 	rm -rf sdk-tools-linux-3859397.zip

# RUN yes | $ANDROID_HOME/tools/bin/sdkmanager --licenses

# # RUN $ANDROID_HOME/tools/bin/sdkmanager --update --verbose
# RUN $ANDROID_HOME/tools/bin/sdkmanager --verbose	\
# 	"platform-tools"								\
# 	"emulator"										\
# 	"build-tools;25.0.3"							\
# 	"platforms;android-19"							\
# 	"system-images;android-19;default;armeabi-v7a"	\
# 	"system-images;android-19;default;x86"

# ENV LD_LIBRARY_PATH=$ANDROID_HOME/emulator/lib64/qt/lib:$ANDROID_HOME/emulator/lib64/gles_mesa

# RUN DEBIAN_FRONTEND=noninteractive apt-get -qq install -y libxdamage1 libxfixes3 qemu-kvm cpu-checker supervisor



# V2 --------------------------------------------------

# TODO: move to packages at top
RUN echo "Preparing Debian packages..." && \
	apt-get -qq update && \
	DEBIAN_FRONTEND=noninteractive apt-get -qq install -y \
	wget supervisor

RUN wget -qO- http://dl.google.com/android/android-sdk_r23-linux.tgz | \
    tar xvz -C /usr/local/ && \
    mv /usr/local/android-sdk-linux /usr/local/android-sdk && \
    chown -R root:root /usr/local/android-sdk/

# Add android tools and platform tools to PATH
ENV ANDROID_HOME /usr/local/android-sdk
ENV PATH $PATH:$ANDROID_HOME/tools
ENV PATH $PATH:$ANDROID_HOME/platform-tools

RUN ( sleep 4 && while [ 1 ]; do sleep 1; echo y; done ) | android update sdk --no-ui --force -a --filter \
    build-tools-24.0.3,tools,platform-tools,android-19,extra-android-m2repository,sys-img-x86-android-19,sys-img-armeabi-v7a-android-19 && \
    echo "y" | android update adb

# Create fake keymap file
RUN mkdir /usr/local/android-sdk/tools/keymaps && \
    touch /usr/local/android-sdk/tools/keymaps/en-us

EXPOSE 22
EXPOSE 5037
EXPOSE 5554
EXPOSE 5555
EXPOSE 5900

#RUN android update sdk --no-ui --all --filter "build-tools-24.0.3"

# RUN yes | /usr/local/android-sdk/tools/bin/sdkmanager --licenses

# COPY ./android/licenses $ANDROID_HOME/licenses

# sdk-build-tools require the license to be accepted
RUN mkdir -p $ANDROID_HOME/licenses && echo -e "\n8933bad161af4178b1185d1a37fbf41ea5269c55\n" > $ANDROID_HOME/licenses/android-sdk-license

RUN mkdir -p /temp

# download the most critical gradle dependencies for the build beforehand
COPY ./shared/build.gradle /temp
COPY ./android/AndroidManifest.xml /temp/src/main/AndroidManifest.xml
WORKDIR /temp
RUN cd /temp && gradle --dry-run
