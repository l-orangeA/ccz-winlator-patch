$adb = "D:\MuMuPlayer\nx_main\adb.exe"
# 物理1080x2408，Read显示按比例换算：显示(760,1855)@897x2000 -> 物理(915,2234) -> 逻辑(*0.909) = (832,2031)
& $adb shell 'input tap 832 2031'
Start-Sleep -Seconds 4
& $adb shell 'screencap -p /sdcard/probe_shot.png'
& $adb pull /sdcard/probe_shot.png 'D:\ccz_winlator_patch\unpack_tools\winlator11\newdev9.png' | Out-Null
Write-Output 'tapped save'
