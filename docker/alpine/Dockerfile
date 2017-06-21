FROM library/openjdk:8u131-alpine

RUN mkdir -p /temp/docker/shared/
WORKDIR /temp/docker/shared/

# NOTE: copy shared scripts and run them separately
# this helps when changing commands only in a single script,
# since it will not requrie rebuilding all docker image layers
# but just the ones that were affected

COPY ./shared/install.alpine.packages.sh /temp/docker/shared
RUN ./install.alpine.packages.sh

COPY ./shared/install.maven.sh /temp/docker/shared
RUN ./install.maven.sh
ENV PATH "$PATH:/opt/apache-maven-3.5.0/bin"

# download the most critical maven dependencies for the build beforehand
COPY ./shared/pom.xml /temp
WORKDIR /temp
RUN export J2V8_PLATFORM_NAME=temp && \
    export J2V8_ARCH_NAME=temp && \
    mvn verify -DskipTests || true
