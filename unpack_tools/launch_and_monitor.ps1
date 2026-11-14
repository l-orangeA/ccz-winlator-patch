$adb = "D:\MuMuPlayer\nx_main\adb.exe"
& $adb shell 'input tap 650 330'
Write-Output 'tapped run, monitoring...'
for ($i = 1; $i -le 15; $i++) {
  Start-Sleep -Seconds 10
  $e = (& $adb shell 'ps -A 2>/dev/null | grep -cE "Ekd5|wfm"').Trim()
  $b = (& $adb shell 'ps -A 2>/dev/null | grep -cE "box64"').Trim()
  $s = (& $adb shell 'run-as com.winlator sh -c "cat files/rootfs/home/xuser-1/hwbp_stats.txt 2>/dev/null"').Trim()
  Write-Output ("t+" + ($i*10) + "s: ekd/wfm=" + $e + " box64=" + $b + " stats=[" + $s + "]")
}
& $adb shell 'screencap -p /sdcard/probe_shot.png'
& $adb pull /sdcard/probe_shot.png 'D:\ccz_winlator_patch\unpack_tools\winlator11\newdev19.png' | Out-Null
Write-Output 'monitor done'
