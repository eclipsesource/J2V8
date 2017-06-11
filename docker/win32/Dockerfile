FROM microsoft/windowsservercore

#LINK : fatal error LNK1318: Unexpected PDB error; RPC (23) '(0x000006E7)' [C:\j2v8\cmake.out\win32.x64\CMakeFiles\CMakeTmp\cmTC_c65fc.vcxproj]

# SHELL ["powershell"]

# RUN Invoke-WebRequest \
#     "https://download.microsoft.com/download/9/B/B/9BB1309E-1A8F-4A47-A6C5-ECF76672A3B3/BuildTools_Full.exe" \
#     -OutFile "$env:TEMP\BuildTools_Full.exe" -UseBasicParsing

# RUN & "$env:TEMP\BuildTools_Full.exe" /Silent /Full
# # Todo: delete the BuildTools_Full.exe file in this layer

# # Note: Add Msbuild to path
# RUN setx PATH '%PATH%;C:\\Program Files (x86)\\MSBuild\\12.0\\Bin\\msbuild.exe'

# MSBuild
# source: https://hub.docker.com/r/nugardt/msbuild/

# SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop'; $ProgressPreference = 'SilentlyContinue';"]
SHELL ["powershell", "-Command", "$ErrorActionPreference = 'Stop';"]

# #Set PATH variable
# RUN $env:PATH = 'C:\Program Files (x86)\Microsoft Visual Studio\2017\BuildTools\MSBuild\15.0\Bin\;' + $env:PATH; \
# [Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::Machine);

# # RUN Invoke-WebRequest "https://download.microsoft.com/download/E/F/D/EFD52638-B804-4865-BB57-47F4B9C80269/NDP462-DevPack-KB3151934-ENU.exe" \
# #     -OutFile "$env:TEMP\NDP462-DevPack-KB3151934-ENU.exe" -UseBasicParsing
# # RUN & cmd /c start /WAIT "$env:TEMP\NDP462-DevPack-KB3151934-ENU.exe /install /quiet"

# # # Download MSBuild v15.0
# # RUN Invoke-WebRequest "https://download.microsoft.com/download/3/A/B/3ABDE7FA-A349-4AF0-A3AC-0D7BB0977A32/vs_BuildTools.exe" -OutFile "$env:TEMP\vs_BuildTools.exe" -UseBasicParsing; \
# # cmd /WAIT /c "$env:TEMP\vs_BuildTools.exe --add Microsoft.VisualStudio.Workload.MSBuildTools --quiet"; \
# # while (!(Test-Path 'C:\Program Files (x86)\Microsoft Visual Studio\2017\BuildTools\MSBuild\15.0\Bin\msbuild.exe')) { Start-Sleep 1 }; \
# # rm "$env:TEMP\vs_BuildTools.exe"

#  Python
# source: https://github.com/docker-library/python/blob/master/2.7/windows/windowsservercore/Dockerfile
ENV PYTHON_VERSION 2.7.13
ENV PYTHON_RELEASE 2.7.13

RUN $url = ('https://www.python.org/ftp/python/{0}/python-{1}.amd64.msi' -f $env:PYTHON_RELEASE, $env:PYTHON_VERSION); \
    Write-Host ('Downloading {0} ...' -f $url); \
    (New-Object System.Net.WebClient).DownloadFile($url, 'python.msi'); \
    \
    Write-Host 'Installing ...'; \
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

#  CMake
ENV CMAKE_VERSION 3.9.0-rc2

RUN $url = ('https://cmake.org/files/v3.9/cmake-{0}-win64-x64.zip' -f $env:CMAKE_VERSION); \
    Write-Host ('Downloading {0} ...' -f $url); \
    (New-Object System.Net.WebClient).DownloadFile($url, 'cmake.zip'); \
    \
    Write-Host 'Installing ...';

RUN Write-Host '  DIR'; dir;

# COPY ./win32/unzip.py C:/temp/

COPY ./win32/unzip.ps1 C:/temp/
RUN C:/temp/unzip.ps1 "cmake.zip" "."

# RUN Add-Type -AssemblyName System.IO.Compression.FileSystem \n\
#     function Unzip \n\
#     { \n\
#         param([string]$zipfile, [string]$outpath) \n\
#         \n\
#         [System.IO.Compression.ZipFile]::ExtractToDirectory($zipfile, $outpath) \n\
#     } \n\
#     Unzip "cmake.zip" "."

# RUN python C:/temp/unzip.py cmake.zip

RUN Write-Host '  DIR'; dir C:\;




# # RUN Invoke-WebRequest "https://download.microsoft.com/download/E/F/D/EFD52638-B804-4865-BB57-47F4B9C80269/NDP462-DevPack-KB3151934-ENU.exe" \
# #     -OutFile "$env:TEMP\NDP462-DevPack-KB3151934-ENU.exe" -UseBasicParsing
# # RUN & cmd /c start /WAIT "$env:TEMP\NDP462-DevPack-KB3151934-ENU.exe /install /quiet"

#Set PATH variable
# RUN $env:PATH = 'C:\Program Files (x86)\Microsoft Visual Studio\2017\BuildTools\MSBuild\15.0\Bin\;' + $env:PATH; \
# [Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::Machine);

# Download MSBuild v15.0
# RUN Invoke-WebRequest "https://download.microsoft.com/download/3/A/B/3ABDE7FA-A349-4AF0-A3AC-0D7BB0977A32/vs_BuildTools.exe" -OutFile "$env:TEMP\vs_BuildTools.exe" -UseBasicParsing;

# RUN cmd /WAIT /c "$env:TEMP\vs_BuildTools.exe --quiet --add Microsoft.VisualStudio.Workload.MSBuildTools"; \
# while (!(Test-Path 'C:\Program Files (x86)\Microsoft Visual Studio\2017\BuildTools\MSBuild\15.0\Bin\msbuild.exe')) { Start-Sleep 1; echo "waiting..." };

# RUN "$env:TEMP\\vs_BuildTools.exe" --quiet --add Microsoft.VisualStudio.Workload.VCTools --includeRecommended

# RUN rm "$env:TEMP\vs_BuildTools.exe"


# installs vs2017 msbuild successfully
# RUN Invoke-WebRequest "https://aka.ms/vs/15/release/vs_BuildTools.exe" -OutFile vs_BuildTools.exe -UseBasicParsing
# RUN Start-Process -FilePath 'vs_BuildTools.exe' -ArgumentList '--quiet', '--norestart', '--add Microsoft.VisualStudio.Workload.MSBuildTools', '--add Microsoft.VisualStudio.Workload.VCTools', '--includeRecommended' -Wait
# RUN Remove-Item .\vs_BuildTools.exe
# RUN Remove-Item -Force -Recurse 'C:\Program Files (x86)\Microsoft Visual Studio\Installer'
# RUN setx /M PATH $($Env:PATH + ';' + ${Env:ProgramFiles(x86)} + '\Microsoft Visual Studio\2017\BuildTools\MSBuild\15.0\Bin')

# TODO
# source: https://github.com/friism/dockerfiles/blob/master/vs-build-tools/17/Dockerfile
# RUN Invoke-WebRequest "http://go.microsoft.com/fwlink/?LinkId=691126" \
#     -OutFile visualcppbuildtools_full.exe -UseBasicParsing ; \
#     Start-Process -FilePath 'visualcppbuildtools_full.exe' -ArgumentList '/quiet', '/NoRestart' -Wait ; \
#     Remove-Item .\visualcppbuildtools_full.exe




# RUN setx /M PATH $($Env:PATH + ';' + ${Env:ProgramFiles(x86)} + '\Microsoft Visual Studio\2017\BuildTools\MSBuild\15.0\Bin')

# RUN python -c 'import zipfile; zip_ref = zipfile.ZipFile(\"cmake.zip\",\"r\"); zip_ref.extractall(\"cmake\"); zip_ref.close();';

# # SHELL ["cmd"]

# RUN Write-Host (Get-Location | select -ExpandProperty Path)'\cmake-'$env:CMAKE_VERSION'-win64-x64\bin;'$env:PATH;

# RUN dir "\"C:\Program Files (x86)\Microsoft Visual Studio\""


# TODO
# RUN $env:PATH = (Get-Location | select -ExpandProperty Path)+'\cmake-'+$env:CMAKE_VERSION+'-win64-x64\bin;'+$env:PATH; \
# [Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::Machine);

# RUN Invoke-WebRequest -Uri "http://go.microsoft.com/fwlink/?LinkId=691126" -OutFile "C:\VisualCppBuildTools_Full.exe" -UseBasicParsing; \
# 	Invoke-Command -ScriptBlock { "C:\VisualCppBuildTools_Full.exe" /full /NoRefresh /norestart /silent /passive } ; \
# 	Wait-Process -Name visualcppbuildtools_full



# RUN dir "\"C:\Program Files (x86)\Microsoft Visual Studio\2017\""
# RUN dir "\"C:\Program Files (x86)\Microsoft Visual Studio\""


# RUN dir "\"C:\Program Files (x86)\Microsoft Visual Studio\2017\BuildTools\Common7\IDE\""

# RUN Write-Host '  DIR'; dir C:\;

# # RUN echo "bababababab " && dir cmake && \
# #         setx PATH '%PATH%;%CD%\\cmake-%CMAKE_VERSION%-win64-x64\\bin'

# RUN Start-Process C:\\cmake-3.9.0-rc2-win64-x64\\bin\\cmake.exe -Wait -ArgumentList @( \
#             '--version' \
#         );
