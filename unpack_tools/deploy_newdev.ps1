$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 部署 hwbp3 ntdll（带原版备份） ==='
& $adb push 'D:\ccz_winlator_patch\build\ntdll_wine10_hwbp3.so' /data/local/tmp/ntdll_hwbp3.so 2>&1 | Out-Null
& $adb shell 'chmod 644 /data/local/tmp/ntdll_hwbp3.so'
& $adb shell 'run-as com.winlator sh -c "ls -la files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so; cp -n files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so.orig; cp /data/local/tmp/ntdll_hwbp3.so files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so; md5sum files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so; echo expect: fe2c472591402be05f148958a07b910e"'
Write-Output '=== 2. 写鼠标跟随模式 prefs ==='
& $adb shell 'am force-stop com.winlator; sleep 1'
$xml = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>`n<map>`n    <boolean name=`"show_shortcuts_first`" value=`"false`" />`n    <string name=`"box64_version`">0.4.4</string>`n    <boolean name=`"move_cursor_to_touchpoint`" value=`"true`" />`n</map>`n"
$b64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($xml))
& $adb shell ('run-as com.winlator sh -c "echo ' + $b64 + ' | base64 -d > shared_prefs/com.winlator_preferences.xml; cat shared_prefs/com.winlator_preferences.xml"')
Write-Output '=== 3. 重启 app ==='
& $adb shell 'monkey -p com.winlator -c android.intent.category.LAUNCHER 1' | Out-Null
Write-Output 'done'
