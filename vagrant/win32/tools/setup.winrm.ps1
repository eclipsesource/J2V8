c:/windows/system32/winrm.cmd quickconfig --force -quiet

winrm get winrm/config/winrs

Set-Item WSMan:\localhost\Plugin\microsoft.powershell\Quotas\MaxProcessesPerShell 50
Set-Item WSMan:\localhost\Plugin\microsoft.powershell\Quotas\MaxMemoryPerShellMB 4096

Set-Item WSMan:\localhost\Shell\MaxProcessesPerShell 50
Set-Item WSMan:\localhost\Shell\MaxMemoryPerShellMB 4096

# Restart the WinRM Service
Restart-Service WinRM

winrm get winrm/config/winrs

echo 1 > C:/Users/IEUser/winrm_ok
