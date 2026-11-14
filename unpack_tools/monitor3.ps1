$adb = "D:\MuMuPlayer\nx_main\adb.exe"
for ($i = 1; $i -le 9; $i++) {
  Start-Sleep -Seconds 10
  $c = (& $adb shell 'ps -A 2>/dev/null | grep -cE "Ekd5"').Trim()
  Write-Output ("t+" + ($i*10) + "s: Ekd5_procs=" + $c)
}
& $adb shell 'run-as com.winlator ls -la files/rootfs/home/xuser-1/ 2>&1 | grep -E "hwbp|stats"'
& $adb shell 'screencap -p /sdcard/probe_shot.png'
& $adb pull /sdcard/probe_shot.png 'D:\ccz_winlator_patch\unpack_tools\winlator11\newdev17.png' | Out-Null
Write-Output 'shot pulled'
