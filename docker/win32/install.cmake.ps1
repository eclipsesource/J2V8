
#  CMake version
$env:CMAKE_VERSION = '3.9.0-rc2';

[Environment]::SetEnvironmentVariable('CMAKE_VERSION', $env:CMAKE_VERSION, [EnvironmentVariableTarget]::Process);

# download CMake archive
$url = ('https://cmake.org/files/v3.9/cmake-{0}-win64-x64.zip' -f $env:CMAKE_VERSION);
Write-Host ('Downloading {0} ...' -f $url);
(New-Object System.Net.WebClient).DownloadFile($url, 'C:\cmake.zip');

# extract CMake archive
Write-Host 'Installing CMake ...';
C:/j2v8/docker/win32/unzip.ps1 "C:/cmake.zip" "C:/"

# add CMake to path
$env:PATH = 'C:\cmake-'+$env:CMAKE_VERSION+'-win64-x64\bin;'+$env:PATH;
[Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::Machine);

Write-Host 'Verifying install ...';
Write-Host 'cmake -version'; cmake -version;

Write-Host 'Removing ...';
Remove-Item C:\cmake.zip -Force;

Write-Host 'Complete.';
