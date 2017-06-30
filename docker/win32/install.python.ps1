
$env:PYTHON_VERSION = '2.7.13';
$env:PYTHON_RELEASE = '2.7.13';

[Environment]::SetEnvironmentVariable('PYTHON_VERSION', $env:PYTHON_VERSION, [EnvironmentVariableTarget]::Process);
[Environment]::SetEnvironmentVariable('PYTHON_RELEASE', $env:PYTHON_RELEASE, [EnvironmentVariableTarget]::Process);

$url = ('https://www.python.org/ftp/python/{0}/python-{1}.amd64.msi' -f $env:PYTHON_RELEASE, $env:PYTHON_VERSION);
Write-Host ('Downloading {0} ...' -f $url);
(New-Object System.Net.WebClient).DownloadFile($url, 'C:\python.msi');

Write-Host 'Installing Python ...';
# https://www.python.org/download/releases/2.4/msi/
Start-Process msiexec -Wait `
    -ArgumentList @(
        '/i',
        'C:\python.msi',
        '/quiet',
        '/qn',
        'TARGETDIR=C:\Python',
        'ALLUSERS=1',
        'ADDLOCAL=DefaultFeature,Extensions,TclTk,Tools,PrependPath'
    );

# the installer updated PATH, so we should refresh our local value
$env:PATH = [Environment]::GetEnvironmentVariable('PATH', [EnvironmentVariableTarget]::Machine);

Write-Host 'Verifying install ...';
Write-Host 'python --version'; python --version;

Write-Host 'Removing ...';
Remove-Item C:\python.msi -Force;

Write-Host 'Complete.';
