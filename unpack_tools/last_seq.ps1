$t = Get-Content 'D:\ccz_winlator_patch\unpack_tools\winlator11\crash2_full.log' -Encoding UTF8 -ErrorAction SilentlyContinue
$seq = $t | Where-Object { $_ -match ' 32107 ' }
Write-Output ("pid32107 lines: " + @($seq).Count)
$i = 0
$skip = [Math]::Max(0, @($seq).Count - 60)
foreach ($l in $seq) {
  $i++
  if ($i -le $skip) { continue }
  $s = $l
  if ($s.Length -gt 210) { $s = $s.Substring(0, 210) }
  Write-Output $s
}
