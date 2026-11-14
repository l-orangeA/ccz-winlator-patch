$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== log size ==='
& $adb shell 'run-as com.winlator ls -la files/rootfs/home/xuser-1/wine_dbg.log'
Write-Output '=== Ekd5 mentions ==='
& $adb shell 'run-as com.winlator grep -n -i ekd files/rootfs/home/xuser-1/wine_dbg.log' | Select-Object -First 20
Write-Output '=== seh/exception lines (tail) ==='
& $adb shell 'run-as com.winlator grep -n -E "seh:|exception|Unhandled|terminat" files/rootfs/home/xuser-1/wine_dbg.log' | Select-Object -Last 30
Write-Output '=== full log tail 50 ==='
& $adb shell 'run-as com.winlator tail -50 files/rootfs/home/xuser-1/wine_dbg.log'
