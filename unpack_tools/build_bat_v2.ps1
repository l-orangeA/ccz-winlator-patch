$lines = @(
  '@echo off',
  'echo === START %TIME% === >> D:\ccz\ekd5_run.log',
  'D:\ccz\Ekd5.exe >> D:\ccz\ekd5_run.log 2>&1',
  'echo === END %TIME% EXITCODE=%ERRORLEVEL% === >> D:\ccz\ekd5_run.log'
)
$bat = ($lines -join "`r`n") + "`r`n"
[System.IO.File]::WriteAllBytes('D:\ccz_winlator_patch\unpack_tools\ekd5_test.bat', [System.Text.Encoding]::ASCII.GetBytes($bat))
Write-Output "written:"
[System.Text.Encoding]::ASCII.GetString([System.IO.File]::ReadAllBytes('D:\ccz_winlator_patch\unpack_tools\ekd5_test.bat'))
