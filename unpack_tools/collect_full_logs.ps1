$adb = "D:\MuMuPlayer\nx_main\adb.exe"
& $adb shell 'input tap 650 330'
Write-Output 'tapped, waiting 90s for shell crash...'
Start-Sleep -Seconds 90
Write-Output '=== procs ==='
& $adb shell 'ps -A 2>/dev/null | grep -E "wineserver|Ekd|box64" | head -5'
Write-Output '=== runlog ==='
& $adb shell 'cat /sdcard/ccz/ekd5_run.log 2>&1'
Write-Output '=== dbg logs ==='
& $adb shell 'run-as com.winlator ls -la files/rootfs/home/xuser-1/wine_dbg_*.log 2>&1'
