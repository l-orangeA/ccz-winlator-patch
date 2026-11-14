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
