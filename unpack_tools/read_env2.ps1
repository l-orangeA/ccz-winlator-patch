$adb = "D:\MuMuPlayer\nx_main\adb.exe"
# tr via od-free approach: use run-as to copy environ into app files, then read via exec-out
& $adb shell 'run-as com.winlator sh -c "cat /proc/23099/environ > files/env23099.txt 2>&1; ls -la files/env23099.txt"'
$bytes = & $adb exec-out run-as com.winlator cat files/env23099.txt
$text = [System.Text.Encoding]::UTF8.GetString($bytes)
$lines = $text -split "`0"
Write-Output ("total env vars: " + $lines.Count)
$lines | Where-Object { $_ -match 'WINE|BOX64' } | ForEach-Object { Write-Output $_ }
