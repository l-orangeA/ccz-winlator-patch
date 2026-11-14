$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 部署 hwbp5 ==='
& $adb push 'D:\ccz_winlator_patch\build\ntdll_wine10_hwbp5.so' /data/local/tmp/ntdll_hwbp5.so 2>&1 | Out-Null
& $adb shell 'chmod 644 /data/local/tmp/ntdll_hwbp5.so; am force-stop com.winlator; sleep 1'
& $adb shell 'run-as com.winlator sh -c "cp /data/local/tmp/ntdll_hwbp5.so files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so; md5sum files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so; echo expect: fcd4c7e3faf30a41e43f285b994d32fd"'
Write-Output '=== 2. 清旧 dbg 日志（保留其它） ==='
& $adb shell 'run-as com.winlator sh -c "rm -f files/rootfs/home/xuser-1/wine_dbg*.log"'
Write-Output '=== 3. 重启 app ==='
& $adb shell 'monkey -p com.winlator -c android.intent.category.LAUNCHER 1' 2>&1 | Out-Null
Write-Output 'restarted - 请点 Container-1 的 ▶，壳会自动启动并崩溃，完后我抓完整日志'
