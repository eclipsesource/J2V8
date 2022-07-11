# sys_image can be overridden from the CLI
ARG sys_image=debian:buster

FROM $sys_image

# vendor can be overridden from the CLI
ARG vendor=debian

RUN mkdir -p /temp/docker/shared/
WORKDIR /temp/docker/shared/

# NOTE: copy shared scripts and run them separately
# this helps when changing commands only in a single script,
# since it will not requrie rebuilding all docker image layers
# but just the ones that were affected

COPY ./shared/install.${vendor}.packages.sh /temp/docker/shared
RUN ./install.${vendor}.packages.sh

COPY ./shared/install.jdk.sh /temp/docker/shared
RUN ./install.jdk.sh
# ENV JAVA_HOME "/opt/jdk/jdk1.8.0_131"

COPY ./shared/install.maven.sh /temp/docker/shared
RUN ./install.maven.sh
ENV PATH "$PATH:/opt/apache-maven-3.5.0/bin"

COPY ./shared/install.cmake.sh /temp/docker/shared
RUN ./install.cmake.sh
ENV PATH "$PATH:/opt/cmake/bin"

# download the most critical maven dependencies for the build beforehand
COPY ./shared/pom.xml /temp
WORKDIR /temp
RUN mvn clean verify || true
