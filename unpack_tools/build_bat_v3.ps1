$lines = @(
  '@echo off',
  'cd /d D:\ccz',
  'echo === START %TIME% CWD=%CD% === >> D:\ccz\ekd5_run.log',
  'D:\ccz\Ekd5.exe >> D:\ccz\ekd5_run.log 2>&1',
  'echo === END %TIME% EXITCODE=%ERRORLEVEL% === >> D:\ccz\ekd5_run.log'
)
$bat = ($lines -join "`r`n") + "`r`n"
[System.IO.File]::WriteAllBytes('D:\ccz_winlator_patch\unpack_tools\ekd5_test.bat', [System.Text.Encoding]::ASCII.GetBytes($bat))
Write-Output "written ok"
