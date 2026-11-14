$adb = "D:\MuMuPlayer\nx_main\adb.exe"
$bytes = & $adb exec-out run-as com.winlator cat files/env24310.txt
[byte[]]$b = $bytes
$text = [System.Text.Encoding]::UTF8.GetString($b)
$lines = $text -split [char]0 | Where-Object { $_.Trim() }
Write-Output ("total vars: " + $lines.Count)
Write-Output "--- WINE* ---"
$lines | Where-Object { $_.StartsWith('WINE') } | ForEach-Object { Write-Output ("  [" + $_ + "]") }
Write-Output "--- BOX64* ---"
$lines | Where-Object { $_.StartsWith('BOX64') } | ForEach-Object { Write-Output ("  [" + $_ + "]") }
Write-Output "--- LC_ALL / TU_DEBUG / MESA_VK ---"
$lines | Where-Object { $_ -match '^(LC_ALL|TU_DEBUG|MESA_VK|ZINK)' } | ForEach-Object { Write-Output ("  [" + $_ + "]") }
