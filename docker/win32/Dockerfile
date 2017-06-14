FROM microsoft/windowsservercore:latest

# SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue';"]
SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop';"]

# copy all utility scripts
COPY ./win32/mem.ps1 C:/temp/
COPY ./win32/unzip.ps1 C:/temp/
COPY ./win32/wget.ps1 C:/temp/

#  Python
# source: https://github.com/docker-library/python/blob/master/2.7/windows/windowsservercore/Dockerfile
ENV PYTHON_VERSION 2.7.13
ENV PYTHON_RELEASE 2.7.13

RUN $url = ('https://www.python.org/ftp/python/{0}/python-{1}.amd64.msi' -f $env:PYTHON_RELEASE, $env:PYTHON_VERSION); \
    Write-Host ('Downloading {0} ...' -f $url); \
    (New-Object System.Net.WebClient).DownloadFile($url, 'python.msi'); \
    \
    Write-Host 'Installing Python ...'; \
# https://www.python.org/download/releases/2.4/msi/
    Start-Process msiexec -Wait \
        -ArgumentList @( \
            '/i', \
            'python.msi', \
            '/quiet', \
            '/qn', \
            'TARGETDIR=C:\Python', \
            'ALLUSERS=1', \
            'ADDLOCAL=DefaultFeature,Extensions,TclTk,Tools,PrependPath' \
        ); \
    \
# the installer updated PATH, so we should refresh our local value
    $env:PATH = [Environment]::GetEnvironmentVariable('PATH', [EnvironmentVariableTarget]::Machine); \
    \
    Write-Host 'Verifying install ...'; \
    Write-Host '  python --version'; python --version; \
    \
    Write-Host 'Removing ...'; \
    Remove-Item python.msi -Force; \
    \
    Write-Host 'Complete.';

#  CMake version
ENV CMAKE_VERSION 3.9.0-rc2

# download CMake archive
RUN $url = ('https://cmake.org/files/v3.9/cmake-{0}-win64-x64.zip' -f $env:CMAKE_VERSION); \
    Write-Host ('Downloading {0} ...' -f $url); \
    (New-Object System.Net.WebClient).DownloadFile($url, 'cmake.zip'); \
    Write-Host 'Installing CMake ...';

# extract CMake archive
RUN C:/temp/unzip.ps1 "cmake.zip" "."

# add CMake to path
RUN $env:PATH = (Get-Location | select -ExpandProperty Path)+'\cmake-'+$env:CMAKE_VERSION+'-win64-x64\bin;'+$env:PATH; \
[Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::Machine);

# source: https://github.com/friism/dockerfiles/blob/master/vs-build-tools/17/Dockerfile
# install MSBuild & C++ build tools
RUN Invoke-WebRequest "http://go.microsoft.com/fwlink/?LinkId=691126" \
    -OutFile visualcppbuildtools_full.exe -UseBasicParsing ;

RUN Start-Process -FilePath 'visualcppbuildtools_full.exe' -ArgumentList '/quiet', '/NoRestart' -Wait ; \
    Remove-Item .\visualcppbuildtools_full.exe

# MSbuild path
# NOTE: can add "\amd64" after "...\Bin" for x64 version of the compiler
RUN $env:PATH = 'C:\Program Files (x86)\MSBuild\14.0\Bin;'+$env:PATH; \
[Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::Machine);

# download JDK
RUN C:/temp/wget.ps1 \
    http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-windows-x64.exe \
    C:\jdk.exe \
    "oraclelicense=accept-securebackup-cookie"

RUN Start-Process C:/jdk.exe -Wait \
        -ArgumentList @('/s', 'ADDLOCAL="ToolsFeature,SourceFeature"');
ENV JAVA_HOME "C:\Program Files\Java\jdk1.8.0_131"

# NOTE: only needed if using the amd64 version of MSBuild
# ENV VCTargetsPath "C:\Program Files (x86)\MSBuild\Microsoft.Cpp\v4.0\v140"

RUN C:/temp/wget.ps1 \
    http://www-eu.apache.org/dist/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.zip \
    C:\maven.zip

RUN C:/temp/unzip.ps1 "maven.zip" "."

RUN $env:PATH = 'C:\apache-maven-3.5.0\bin;'+$env:PATH; \
[Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::Machine);

# download the most critical maven dependencies for the build beforehand
# TODO: for some reason this does not work with the win32 docker server image
COPY ./shared/pom.xml C:/temp
WORKDIR /temp
RUN set MVN_PLATFORM_NAME=temp; \
    set MVN_ARCH_NAME=temp; \
    Invoke-Command { mvn verify -DskipTests } -ErrorAction SilentlyContinue
