$adb = "D:\MuMuPlayer\nx_main\adb.exe"
& $adb logcat -d > 'D:\ccz_winlator_patch\unpack_tools\winlator11\crash_full.log' 2>&1
$t = Get-Content 'D:\ccz_winlator_patch\unpack_tools\winlator11\crash_full.log' -Encoding UTF8 -ErrorAction SilentlyContinue
Write-Output ("lines: " + $t.Count)
$hits = $t | Where-Object { $_ -match 'winlator' -and $_ -match ' E | F | fatal|Exception|Error|crash|SIGSEGV|tombstone' }
Write-Output ("error-ish: " + @($hits).Count)
$i = 0
foreach ($h in $hits) {
  $i++
  if ($i -gt 40) { break }
  $s = $h
  if ($s.Length -gt 230) { $s = $s.Substring(0, 230) }
  Write-Output $s
}
