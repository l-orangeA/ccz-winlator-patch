$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. push 产物 ==='
& $adb push 'D:\ccz_winlator_patch\build\skipint3\ntdll_i386_windows.dll' /data/local/tmp/ntdll_i386.dll 2>&1 | Select-String 'pushed'
& $adb push 'D:\ccz_winlator_patch\build\skipint3\ntdll_x64_windows.dll' /data/local/tmp/ntdll_x64.dll 2>&1 | Select-String 'pushed'
& $adb shell 'chmod 644 /data/local/tmp/ntdll_i386.dll /data/local/tmp/ntdll_x64.dll; am force-stop com.winlator; sleep 2'
Write-Output '=== 2. 部署（备份原版） ==='
& $adb shell 'run-as com.winlator sh -c "cd files/rootfs/opt/wine/lib/wine && cp -n i386-windows/ntdll.dll i386-windows/ntdll.dll.orig && cp -n x86_64-windows/ntdll.dll x86_64-windows/ntdll.dll.orig; cp /data/local/tmp/ntdll_i386.dll i386-windows/ntdll.dll; cp /data/local/tmp/ntdll_x64.dll x86_64-windows/ntdll.dll; md5sum i386-windows/ntdll.dll x86_64-windows/ntdll.dll"'
Write-Output '=== 3. 清壳日志（干净抓取） ==='
& $adb shell 'run-as com.winlator sh -c "rm -f files/rootfs/home/xuser-1/wine_dbg_*.log"'
Write-Output '=== 4. 重启 app ==='
& $adb shell 'monkey -p com.winlator -c android.intent.category.LAUNCHER 1' 2>&1 | Out-Null
Write-Output 'restarted'
