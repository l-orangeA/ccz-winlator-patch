$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== monitoring 120s ==='
for ($i = 1; $i -le 12; $i++) {
  Start-Sleep -Seconds 10
  $e = (& $adb shell 'ps -A 2>/dev/null | grep -cE "wfm|Ekd5"').Trim()
  $w = (& $adb shell 'ps -A 2>/dev/null | grep -cE "wineserver"').Trim()
  Write-Output ("t+" + ($i*10) + "s: shell=" + $e + " wineserver=" + $w)
}
Write-Output '=== ekd5_run.log ==='
& $adb shell 'cat /sdcard/ccz/ekd5_run.log 2>&1'
Write-Output '=== dbg logs ==='
& $adb shell 'run-as com.winlator ls -la files/rootfs/home/xuser-1/wine_dbg_*.log 2>&1'
& $adb shell 'run-as com.winlator sh -c "cat files/rootfs/home/xuser-1/hwbp_stats.txt 2>&1"'
& $adb shell 'screencap -p /sdcard/probe_shot.png'
& $adb pull /sdcard/probe_shot.png 'D:\ccz_winlator_patch\unpack_tools\winlator11\skipint3_result2.png' | Out-Null
Write-Output 'done'
