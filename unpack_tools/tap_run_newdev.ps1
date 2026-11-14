$adb = "D:\MuMuPlayer\nx_main\adb.exe"
# ▶ 按钮截图(650,337) 物理 -> 逻辑 *0.909 = (591,306)
& $adb shell 'input tap 591 306'
Start-Sleep -Seconds 60
& $adb shell 'screencap -p /sdcard/probe_shot.png'
& $adb pull /sdcard/probe_shot.png 'D:\ccz_winlator_patch\unpack_tools\winlator11\newdev11.png' | Out-Null
& $adb shell 'ps -A 2>/dev/null | grep -cE "wineserver|winedevice|box64"'
Write-Output 'tapped run'
