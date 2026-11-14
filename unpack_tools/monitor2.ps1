$adb = "D:\MuMuPlayer\nx_main\adb.exe"
for ($i = 1; $i -le 15; $i++) {
  Start-Sleep -Seconds 10
  $c = (& $adb shell 'ps -A 2>/dev/null | grep -E "Ekd5" | head -2').Trim()
  $s = (& $adb shell 'run-as com.winlator sh -c "cat files/rootfs/home/xuser-1/hwbp_stats.txt 2>/dev/null"').Trim()
  $u = (& $adb shell 'run-as com.winlator sh -c "wc -l < files/rootfs/home/xuser-1/hwbp_upd.log 2>/dev/null"').Trim()
  Write-Output ("t+" + ($i*10) + "s: [" + $c + "] stats=[" + $s + "] upd_lines=" + $u)
}
