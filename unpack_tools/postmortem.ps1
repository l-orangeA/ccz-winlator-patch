$adb = "D:\MuMuPlayer\nx_main\adb.exe"
& $adb logcat -d > 'D:\ccz_winlator_patch\unpack_tools\winlator11\crash2_full.log' 2>&1
$t = Get-Content 'D:\ccz_winlator_patch\unpack_tools\winlator11\crash2_full.log' -Encoding UTF8 -ErrorAction SilentlyContinue
Write-Output ("lines: " + $t.Count)
Write-Output '=== FATAL / NPE / crash ==='
$hits = $t | Where-Object { $_ -match 'FATAL|NullPointer|SIGSEGV|SIGABRT|has died|ANR ' }
$i = 0
foreach ($h in $hits) {
  $i++
  if ($i -gt 25) { break }
  $s = $h
  if ($s.Length -gt 240) { $s = $s.Substring(0, 240) }
  Write-Output $s
}
Write-Output '=== winlator activity/procs ==='
$a = $t | Where-Object { $_ -match 'com.winlator' -and $_ -match 'died|Kill|finish|Force' }
$j = 0
foreach ($x in $a) {
  $j++
  if ($j -gt 15) { break }
  $s = $x
  if ($s.Length -gt 220) { $s = $s.Substring(0, 220) }
  Write-Output $s
}
