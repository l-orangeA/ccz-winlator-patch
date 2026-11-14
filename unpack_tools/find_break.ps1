$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 15:26 时段 com.winlator 全部日志（启动序列断点） ==='
$t = Get-Content 'D:\ccz_winlator_patch\unpack_tools\winlator11\crash2_full.log' -Encoding UTF8 -ErrorAction SilentlyContinue
$seq = $t | Where-Object { $_ -match '15:2[56]:' -and $_ -match 'winlator' }
Write-Output ("seq lines: " + @($seq).Count)
$i = 0
foreach ($l in $seq) {
  $i++
  if ($i -gt 80) { break }
  $s = $l
  if ($s.Length -gt 200) { $s = $s.Substring(0, 200) }
  Write-Output $s
}
