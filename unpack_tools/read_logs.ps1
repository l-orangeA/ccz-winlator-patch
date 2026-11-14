$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== ekd5_run.log ==='
& $adb shell 'cat /sdcard/ccz/ekd5_run.log 2>&1'
Write-Output '=== hwbp files ==='
& $adb shell 'run-as com.winlator ls -la files/rootfs/home/xuser-1/ 2>&1'
Write-Output '=== hwbp_upd.log ==='
& $adb shell 'run-as com.winlator sh -c "head -25 files/rootfs/home/xuser-1/hwbp_upd.log 2>&1"'
Write-Output '=== hwbp_stats.txt ==='
& $adb shell 'run-as com.winlator sh -c "cat files/rootfs/home/xuser-1/hwbp_stats.txt 2>&1"'
