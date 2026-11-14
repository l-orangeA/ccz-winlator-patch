$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 停容器 ==='
& $adb shell 'am force-stop com.winlator; sleep 2'
Write-Output '=== 2. Ekd5.exe 中转到 /data/local/tmp ==='
& $adb push 'D:\ccz\曹操传加强版2.2\Ekd5.exe' /data/local/tmp/Ekd5.exe 2>&1 | Select-String 'pushed|error'
& $adb shell 'chmod 644 /data/local/tmp/Ekd5.exe'
Write-Output '=== 3. 替换 wfm.exe（备份原版） ==='
& $adb shell 'run-as com.winlator sh -c "cd files/rootfs/home/xuser-1/.wine/drive_c/windows && cp -n wfm.exe wfm.exe.orig; cp /data/local/tmp/Ekd5.exe wfm.exe; ls -la wfm.exe wfm.exe.orig"'
& $adb shell 'rm -f /data/local/tmp/Ekd5.exe'
Write-Output '=== 4. 启动 app ==='
& $adb shell 'monkey -p com.winlator -c android.intent.category.LAUNCHER 1' 2>&1 | findstr injected
