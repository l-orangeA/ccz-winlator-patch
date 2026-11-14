$adb = "D:\MuMuPlayer\nx_main\adb.exe"
# copy environ from device and parse locally (exec-out is binary-safe)
$bytes = & $adb exec-out run-as com.winlator cat files/env24310.txt
$text = [System.Text.Encoding]::UTF8.GetString($bytes)
$lines = $text -split "`0" | Where-Object { $_ }
Write-Output ("total: " + $lines.Count)
Write-Output "--- WINE* ---"
$lines | Where-Object { $_ -like 'WINE*' } | ForEach-Object { Write-Output $_ }
Write-Output "--- BOX64* ---"
$lines | Where-Object { $_ -like 'BOX64*' } | ForEach-Object { Write-Output $_ }
