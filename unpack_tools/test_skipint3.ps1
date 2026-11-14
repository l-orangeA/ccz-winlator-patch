$adb = "D:\MuMuPlayer\nx_main\adb.exe"
& $adb shell 'input tap 650 330'
Start-Sleep -Seconds 12
$c1 = (& $adb shell 'ps -A 2>/dev/null | grep -cE "winedevice|wineserver"').Trim()
Write-Output ("12s: wine procs=" + $c1)
if ($c1 -eq "0") {
  Write-Output 'tap failed - trying second position'
  & $adb shell 'input tap 591 306'
  Start-Sleep -Seconds 12
}
Write-Output '=== monitoring shell survival 120s ==='
for ($i = 1; $i -le 12; $i++) {
  Start-Sleep -Seconds 10
  $e = (& $adb shell 'ps -A 2>/dev/null | grep -cE "wfm|Ekd5|ntvdm"').Trim()
  $w = (& $adb shell 'ps -A 2>/dev/null | grep -cE "wineserver"').Trim()
  Write-Output ("t+" + ($i*10) + "s: shell_procs=" + $e + " wineserver=" + $w)
}
& $adb shell 'screencap -p /sdcard/probe_shot.png'
& $adb pull /sdcard/probe_shot.png 'D:\ccz_winlator_patch\unpack_tools\winlator11\skipint3_test.png' | Out-Null
Write-Output 'monitor done'
