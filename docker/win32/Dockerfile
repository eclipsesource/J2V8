# sys_image can be overridden from the CLI
ARG sys_image=microsoft/windowsservercore:latest

FROM $sys_image

# SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue';"]
SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop';"]

# copy all utility scripts before the actual install scripts
COPY ./win32/mem.ps1 C:/j2v8/docker/win32/
COPY ./win32/unzip.ps1 C:/j2v8/docker/win32/
COPY ./win32/wget.ps1 C:/j2v8/docker/win32/

# Python
COPY ./win32/install.python.ps1 C:/j2v8/docker/win32/
RUN C:/j2v8/docker/win32/install.python.ps1

# VS C++
COPY ./win32/install.vscpp.ps1 C:/j2v8/docker/win32/
RUN C:/j2v8/docker/win32/install.vscpp.ps1

# CMake
COPY ./win32/install.cmake.ps1 C:/j2v8/docker/win32/
RUN C:/j2v8/docker/win32/install.cmake.ps1

# JDK
COPY ./win32/install.jdk.ps1 C:/j2v8/docker/win32/
RUN C:/j2v8/docker/win32/install.jdk.ps1

# Maven
COPY ./win32/install.maven.ps1 C:/j2v8/docker/win32/
RUN C:/j2v8/docker/win32/install.maven.ps1

# NOTE: only needed if using the amd64 version of MSBuild
# ENV VCTargetsPath "C:\Program Files (x86)\MSBuild\Microsoft.Cpp\v4.0\v140"

# NOTE: need to remove temporary j2v8 dir, since at the same directory the docker volume will be mounted
RUN Remove-Item -Recurse -Force C:/j2v8

# download the most critical maven dependencies for the build beforehand
# TODO: for some reason this does not cache the maven downloads in the win32 docker server image
RUN mkdir C:/temp
COPY ./shared/pom.xml C:/temp
WORKDIR /temp
RUN Invoke-Command { mvn clean verify } -ErrorAction SilentlyContinue
