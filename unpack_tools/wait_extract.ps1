$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 轮询解压进度（最长 8 分钟） ==='
for ($i = 1; $i -le 16; $i++) {
  Start-Sleep -Seconds 30
  $n = (& $adb shell 'run-as com.winlator find files/rootfs -type f 2>/dev/null | wc -l').Trim()
  $v = (& $adb shell 'run-as com.winlator ls files/rootfs/opt/wine/version 2>/dev/null | wc -l').Trim()
  Write-Output ("t+" + ($i*30) + "s: files=" + $n + " version_present=" + $v)
  if ([int]$n -gt 10000 -and $v -eq "1") {
    Write-Output 'EXTRACTION_COMPLETE'
    break
  }
}
& $adb shell 'screencap -p /sdcard/probe_shot.png'
& $adb pull /sdcard/probe_shot.png 'D:\ccz_winlator_patch\unpack_tools\winlator11\extract_state.png' | Out-Null
Write-Output 'poll done'
