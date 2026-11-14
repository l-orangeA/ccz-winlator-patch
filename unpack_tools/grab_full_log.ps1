$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== dbg logs ==='
& $adb shell 'run-as com.winlator ls -la files/rootfs/home/xuser-1/wine_dbg_*.log 2>&1'
Write-Output '=== 最大的日志（壳进程）尾部 120 行 ==='
& $adb shell 'run-as com.winlator sh -c "ls -S files/rootfs/home/xuser-1/wine_dbg_*.log 2>/dev/null | head -3"'
Write-Output '=== 全部拉回 ==='
& $adb shell 'run-as com.winlator sh -c "cd files/rootfs/home/xuser-1 && for f in wine_dbg_*.log; do cp $f /data/local/tmp/ 2>/dev/null; done; chmod 644 /data/local/tmp/wine_dbg_*.log; ls /data/local/tmp/wine_dbg_*.log 2>&1"'
& $adb shell 'mkdir -p /sdcard/dbglogs 2>/dev/null; cp /data/local/tmp/wine_dbg_*.log /sdcard/dbglogs/ 2>/dev/null; ls -la /sdcard/dbglogs/ 2>&1'
