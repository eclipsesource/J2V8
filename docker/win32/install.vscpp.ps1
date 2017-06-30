# source: https://github.com/friism/dockerfiles/blob/master/vs-build-tools/17/Dockerfile
# install MSBuild & C++ build tools
Invoke-WebRequest "http://go.microsoft.com/fwlink/?LinkId=691126" `
    -OutFile C:\visualcppbuildtools_full.exe -UseBasicParsing;

Write-Host 'Installing VS C++ ...';
Start-Process -FilePath 'C:\visualcppbuildtools_full.exe' -ArgumentList '/quiet', '/NoRestart' -Wait;

# MSbuild path
# NOTE: can add "\amd64" after "...\Bin" for x64 version of the compiler
$env:PATH = 'C:\Program Files (x86)\MSBuild\14.0\Bin;'+$env:PATH;
[Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::Machine);

Write-Host 'Verifying install ...';
Write-Host 'msbuild /version'; msbuild /version;

Write-Host 'Removing ...';
Remove-Item C:\visualcppbuildtools_full.exe -Force;

Write-Host 'Complete.';
