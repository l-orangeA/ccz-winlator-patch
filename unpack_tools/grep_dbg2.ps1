$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== log size ==='
& $adb shell 'run-as com.winlator ls -la files/rootfs/home/xuser-1/wine_dbg.log'
Write-Output '=== Ekd5 mentions (first 20) ==='
$r1 = & $adb shell 'run-as com.winlator grep -c -i ekd files/rootfs/home/xuser-1/wine_dbg.log'
Write-Output ("ekd mentions: " + $r1)
$r2 = & $adb shell 'run-as com.winlator grep -n -i ekd files/rootfs/home/xuser-1/wine_dbg.log | head -15'
$r2 | ForEach-Object { Write-Output $_ }
Write-Output '=== seh/exception lines ==='
$r3 = & $adb shell 'run-as com.winlator grep -c -E "seh:|Unhandled|terminat" files/rootfs/home/xuser-1/wine_dbg.log'
Write-Output ("seh/term lines: " + $r3)
$r4 = & $adb shell 'run-as com.winlator grep -n -E "seh:|Unhandled|terminat" files/rootfs/home/xuser-1/wine_dbg.log | tail -20'
$r4 | ForEach-Object { Write-Output $_ }
Write-Output '=== full tail 40 ==='
$r5 = & $adb shell 'run-as com.winlator tail -40 files/rootfs/home/xuser-1/wine_dbg.log'
$r5 | ForEach-Object { Write-Output $_ }
