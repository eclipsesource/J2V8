# download JDK
Write-Host 'Downloading ...';
C:/j2v8/docker/win32/wget.ps1 `
    http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-windows-x64.exe `
    C:\jdk.exe `
    "oraclelicense=accept-securebackup-cookie"

Write-Host 'Installing JDK ...';
Start-Process C:/jdk.exe -Wait `
        -ArgumentList @('/s', 'ADDLOCAL="ToolsFeature,SourceFeature"');

$env:JAVA_HOME = 'C:\Program Files\Java\jdk1.8.0_131';
[Environment]::SetEnvironmentVariable('JAVA_HOME', $env:JAVA_HOME, [EnvironmentVariableTarget]::Machine);

# add Java tools to path
$env:PATH = $env:JAVA_HOME+'\bin;'+$env:PATH;
[Environment]::SetEnvironmentVariable('PATH', $env:PATH, [EnvironmentVariableTarget]::Machine);

Write-Host 'Removing ...';
Remove-Item C:\jdk.exe -Force;

Write-Host 'Complete.';
