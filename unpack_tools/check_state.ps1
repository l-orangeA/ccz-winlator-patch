$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== ekd5_run.log ==='
& $adb shell 'cat /sdcard/ccz/ekd5_run.log 2>&1'
Write-Output '=== wine_dbg files ==='
& $adb shell 'run-as com.winlator ls -la files/rootfs/home/xuser-1/wine_dbg_*.log 2>&1'
Write-Output '=== screenshot ==='
& $adb shell 'screencap -p /sdcard/probe_shot.png'
& $adb pull /sdcard/probe_shot.png 'D:\ccz_winlator_patch\unpack_tools\winlator11\skipint3_state.png' | Out-Null
Write-Output 'done'
