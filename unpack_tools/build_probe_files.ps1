$bat = "@echo off`r`necho === EKD5 TEST START === >> D:\ccz\ekd5_run.log`r`nD:\ccz\Ekd5.exe >> D:\ccz\ekd5_run.log 2>&1`r`necho EXITCODE=%ERRORLEVEL% >> D:\ccz\ekd5_run.log`r`n"
[System.IO.File]::WriteAllBytes('D:\ccz_winlator_patch\unpack_tools\ekd5_test.bat', [System.Text.Encoding]::ASCII.GetBytes($bat))
$ini = "[windows]`r`nrun=D:\ccz\ekd5_test.bat`r`n`r`n[mail]`r`nmapi=1`r`n"
[System.IO.File]::WriteAllBytes('D:\ccz_winlator_patch\unpack_tools\win.ini.new', [System.Text.Encoding]::ASCII.GetBytes($ini))
Get-Item 'D:\ccz_winlator_patch\unpack_tools\ekd5_test.bat', 'D:\ccz_winlator_patch\unpack_tools\win.ini.new' | Select-Object Length, Name | Format-Table -AutoSize
Write-Output '--- bat content ---'
[System.Text.Encoding]::ASCII.GetString([System.IO.File]::ReadAllBytes('D:\ccz_winlator_patch\unpack_tools\ekd5_test.bat'))
