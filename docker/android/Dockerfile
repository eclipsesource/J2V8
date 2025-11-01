ARG sys_image=debian:bullseye-slim

FROM $sys_image

RUN mkdir -p /temp/docker/shared/
WORKDIR /temp/docker/shared/

# NOTE: copy shared scripts and run them separately
# this helps when changing commands only in a single script,
# since it will not requrie rebuilding all docker image layers
# but just the ones that were affected

COPY ./shared/install.debian.packages.sh /temp/docker/shared
RUN ./install.debian.packages.sh

ENV ANDROID_HOME "/usr/local/android-sdk"

ENV NDK_VERSION "r26d"
ENV NDK_NAME "android-ndk-$NDK_VERSION-linux"
RUN echo "Preparing Android NDK..." && \
    mkdir -p /build && \
    cd /build && \
    echo "Downloading $NDK_NAME.zip from Google..." && \
    wget --progress=bar:force https://dl.google.com/android/repository/$NDK_NAME.zip -O $NDK_NAME.zip && \
    echo "Download complete. File size:" && \
    ls -lh $NDK_NAME.zip && \
    echo "Extracting NDK..." && \
    unzip -q $NDK_NAME.zip && \
    echo "Cleaning up..." && \
    rm $NDK_NAME.zip && \
    echo "NDK ready at /build/android-ndk-$NDK_VERSION"

RUN echo "Preparing Android GCC-Toolchain..." && \
    mkdir -p /build && \
    cd /build && \
    git clone https://github.com/sjitech/android-gcc-toolchain
ENV NDK "/build/android-ndk-$NDK_VERSION"
ENV PATH "$PATH:/build/android-gcc-toolchain:$NDK"

ENV PATH "$PATH:$ANDROID_HOME/tools"
ENV PATH "$PATH:$ANDROID_HOME/platform-tools"

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    cd ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip -qq cmdline-tools.zip && \
    mv cmdline-tools latest && \
    rm cmdline-tools.zip

COPY ./shared/install.jdk.sh /temp/docker/shared
RUN ./install.jdk.sh

# Debug: Show what actually got installed and set JAVA_HOME dynamically
RUN echo "=== Debugging Java Installation ===" && \
    ls -la /usr/lib/jvm/ && \
    echo "=== Which javac ===" && \
    which javac && \
    echo "=== Javac real path ===" && \
    readlink -f $(which javac) && \
    echo "=== Java version ===" && \
    java -version

# Set JAVA_HOME dynamically based on actual architecture
RUN ARCH=$(dpkg --print-architecture) && \
    echo "Detected architecture: $ARCH" && \
    echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-$ARCH" >> /etc/environment && \
    echo "JAVA_HOME=/usr/lib/jvm/java-17-openjdk-$ARCH" > /tmp/java_home.env

ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"

ENV PATH ${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

COPY ./shared/install-android-dependencies.sh /temp/docker/shared
RUN ./install-android-dependencies.sh

COPY ./shared/install.cmake.sh /temp/docker/shared
RUN ./install.cmake.sh
ENV PATH "$PATH:/opt/cmake/bin"

COPY ./shared/install.gradle.sh /temp/docker/shared
RUN ./install.gradle.sh
ENV GRADLE_HOME "/opt/gradle-8.5"
ENV PATH "$PATH:$GRADLE_HOME/bin"

RUN mkdir -p /temp
COPY ./shared/build.gradle /temp
COPY ./android/AndroidManifest.xml /temp/src/main/AndroidManifest.xml
WORKDIR /temp
RUN cd /temp && gradle --dry-run

# Note: Removed deprecated Android SDK packages (emulator, tools, system-images)
# These are no longer available in modern Android SDK and not needed for building J2V8

ARG KEYSTORE
ARG KEYSTORE_PASSWORD

ENV KEYSTORE=$KEYSTORE
ENV KEYSTORE_PASSWORD=$KEYSTORE_PASSWORD

RUN mkdir ~/.gnupg

RUN if [ -n "$KEYSTORE" ]; then echo $KEYSTORE | base64 -d > my-private-key.asc; fi
RUN if [ -n "$KEYSTORE" -a -n "$KEYSTORE_PASSWORD" ]; then echo $KEYSTORE_PASSWORD | gpg --import --batch --yes --passphrase-fd 0 my-private-key.asc; fi

RUN cp -r ~/.gnupg /

EXPOSE 22
EXPOSE 5037
EXPOSE 5554
EXPOSE 5555
EXPOSE 5900
