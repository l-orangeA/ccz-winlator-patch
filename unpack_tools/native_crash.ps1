$adb = "D:\MuMuPlayer\nx_main\adb.exe"
$t = Get-Content 'D:\ccz_winlator_patch\unpack_tools\winlator11\crash2_full.log' -Encoding UTF8 -ErrorAction SilentlyContinue
Write-Output '=== Fatal signal / DEBUG / tombstone ==='
$hits = $t | Where-Object { $_ -match 'Fatal signal|SIGSEGV|SIGABRT|SIGBUS|tombstone|Build fingerprint|backtrace:' }
$i = 0
foreach ($h in $hits) {
  $i++
  if ($i -gt 50) { break }
  $s = $h
  if ($s.Length -gt 240) { $s = $s.Substring(0, 240) }
  Write-Output $s
}
Write-Output ("total: " + @($hits).Count)
