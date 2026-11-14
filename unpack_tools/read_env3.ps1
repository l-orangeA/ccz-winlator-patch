$adb = "D:\MuMuPlayer\nx_main\adb.exe"
# get current pids
$pidline = & $adb shell 'pidof wineserver winedevice.exe'
Write-Output ("pids: " + $pidline)
$pids = $pidline -split '\s+' | Where-Object { $_ }
foreach ($p in $pids) {
  & $adb shell ("run-as com.winlator sh -c 'cat /proc/" + $p + "/environ > files/env" + $p + ".txt 2>&1; ls -la files/env" + $p + ".txt'")
  $bytes = & $adb exec-out run-as com.winlator cat files/env$p.txt
  $text = [System.Text.Encoding]::UTF8.GetString($bytes)
  if ($text -match 'No such file') { Write-Output ("pid " + $p + ": environ unreadable/gone"); continue }
  $lines = $text -split "`0" | Where-Object { $_ }
  Write-Output ("pid " + $p + ": " + $lines.Count + " vars")
  $lines | Where-Object { $_ -match 'WINE|BOX64' } | ForEach-Object { Write-Output ("  " + $_) }
}
