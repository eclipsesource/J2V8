
Write-Host 'Downloading ...';
C:/j2v8/docker/win32/wget.ps1 `
    http://www-eu.apache.org/dist/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.zip `
    C:\maven.zip

Write-Host 'Installing Maven ...';
C:/j2v8/docker/win32/unzip.ps1 "C:/maven.zip" "C:/"

$env:PATH = 'C:\apache-maven-3.5.0\bin;'+$env:PATH;
[Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::Machine);

Write-Host 'Verifying install ...';
Write-Host 'mvn -version'; mvn -version;

Write-Host 'Removing ...';
Remove-Item C:\maven.zip -Force;

Write-Host 'Complete.';
