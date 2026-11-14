foreach ($f in @('dbg_13479','dbg_13512','dbg_13699')) {
  $t = Get-Content ("D:\ccz_winlator_patch\unpack_tools\winlator11\" + $f + ".log") -Raw -Encoding UTF8
  $ekd = [regex]::Matches($t, '(?i)ekd').Count
  $disp = [regex]::Matches($t, 'dispatch_exception').Count
  $first = (($t -split "`n")[0..1]) -join ' | '
  if ($first.Length -gt 220) { $first = $first.Substring(0,220) }
  Write-Output ($f + ': ekd=' + $ekd + ' dispatch=' + $disp)
  Write-Output ('  head: ' + $first)
}
