$adb = "D:\MuMuPlayer\nx_main\adb.exe"
for ($i = 1; $i -le 12; $i++) {
  Start-Sleep -Seconds 10
  $c = (& $adb shell 'ps -A 2>/dev/null | grep -cE "Ekd5|box64"').Trim()
  $s = (& $adb shell 'run-as com.winlator ls files/rootfs/home/xuser-1/hwbp_stats.txt 2>/dev/null | wc -l').Trim()
  Write-Output ("t+" + ($i*10) + "s: procs=" + $c + " stats=" + $s)
}
