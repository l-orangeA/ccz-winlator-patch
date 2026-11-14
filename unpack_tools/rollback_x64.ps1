$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 回滚 x64 ntdll.dll 到官方原版 ==='
& $adb shell 'am force-stop com.winlator; sleep 1'
& $adb shell 'run-as com.winlator sh -c "cd files/rootfs/opt/wine/lib/wine && ls -la x86_64-windows/ntdll.dll.orig && cp x86_64-windows/ntdll.dll.orig x86_64-windows/ntdll.dll; md5sum x86_64-windows/ntdll.dll x86_64-windows/ntdll.dll.orig; md5sum i386-windows/ntdll.dll; echo i386-expect: 857bccc54d764725ce85feebe4e7c9e5"'
Write-Output '=== 2. 重启 app ==='
& $adb shell 'monkey -p com.winlator -c android.intent.category.LAUNCHER 1' 2>&1 | Out-Null
Write-Output 'restarted - 请点 CCZ 的 ▶'
