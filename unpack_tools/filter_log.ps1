[Console]::OutputEncoding = [Text.Encoding]::UTF8
$t = Get-Content 'C:\Users\MECHREV\AppData\Local\Temp\zcode-bash-3xOeev\sess_4ca3ec17-3033-4b80-8488-d3e2315b859d\call_d61eb2087b7d40ca9f279f62-stdout.log' -Encoding UTF8
$lines = $t | Where-Object { $_ -match '^\S+ \S+ [EW] ' -and $_ -match 'winlator|container|XServer|wine' }
Write-Output ("matched: " + $lines.Count)
$i = 0
foreach ($l in $lines) {
  $i++
  if ($i -gt 50) { break }
  $s = $l
  if ($s.Length -gt 250) { $s = $s.Substring(0, 250) }
  Write-Output $s
}
