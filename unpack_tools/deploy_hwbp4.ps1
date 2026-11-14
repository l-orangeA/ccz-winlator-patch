$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 部署 hwbp4 ==='
& $adb push 'D:\ccz_winlator_patch\build\ntdll_wine10_hwbp4.so' /data/local/tmp/ntdll_hwbp4.so 2>&1 | Out-Null
& $adb shell 'chmod 644 /data/local/tmp/ntdll_hwbp4.so; am force-stop com.winlator; sleep 1'
& $adb shell 'run-as com.winlator sh -c "cp /data/local/tmp/ntdll_hwbp4.so files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so; md5sum files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so; echo expect: 33a3456a1c70f2a34045f27bcdc39d69"'
Write-Output '=== 2. 调试环境：WINEDEBUG=+loaddll,+seh + WINE_DBG_LOG=1 ==='
& $adb shell 'run-as com.winlator sh -c "sed -i s/WINEDEBUG=-all/WINEDEBUG=+loaddll,+seh\ WINE_DBG_LOG=1/ files/rootfs/home/xuser-1/.container; grep -o \"WINEDEBUG=[^\\\"]*\" files/rootfs/home/xuser-1/.container"'
Write-Output '=== 3. 清旧日志 ==='
& $adb shell 'run-as com.winlator sh -c "rm -f files/rootfs/home/xuser-1/wine_dbg.log files/rootfs/home/xuser-1/hwbp_upd.log"; truncate -s 0 /sdcard/ccz/ekd5_run.log 2>/dev/null; rm -f /sdcard/ccz/ekd5_run.log'
& $adb shell 'monkey -p com.winlator -c android.intent.category.LAUNCHER 1' | Out-Null
Write-Output 'app restarted - tap Run when ready'
