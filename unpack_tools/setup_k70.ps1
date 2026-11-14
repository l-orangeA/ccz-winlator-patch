$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 停 app + 注入容器配置（免 UI） ==='
& $adb shell 'am force-stop com.winlator; sleep 1'
$cfg = '{"id":1,"name":"ccz","screenSize":"1280x720","envVars":"ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB mesa_glthread=true WINEESYNC=1 TU_DEBUG=noconform LC_ALL=zh_CN.utf8 WINEDEBUG=-all BOX64_DYNAREC_BIGBLOCK=3 BOX64_DYNAREC_FORWARD=1024 BOX64_DYNAREC_CALLRET=1 WINE_SS=0xe00 WINE_HWBP_SOFT=1","cpuList":"0,1,2,3","cpuListWoW64":"4,5,6,7","graphicsDriver":"turnip,gladio","dxwrapper":"dxvk","graphicsDriverConfig":"version=26.2.0,maxDeviceMemory=0,directRendering=1,presentMode=1|","audioDriver":"alsa","wincomponents":"direct3d=1,directsound=1,directmusic=1,directshow=0,directplay=0,xaudio=1,vcrun2005=0,vcrun2010=1,wmdecoder=1","drives":"D:/storage/emulated/0/ccz","hudMode":0,"startupSelection":1,"box64Preset":"COMPATIBILITY","desktopTheme":"LIGHT,IMAGE,#0277BD,0","extraData":{"appVersion":"33","rfsVersion":"23","dxwrapper":"dxvk-2.4.1-vkd3d-2.14.1-wined3d","wincomponents":"direct3d=1,directsound=1,directmusic=1,directshow=0,directplay=0,xaudio=1,vcrun2005=0,vcrun2010=1,wmdecoder=1","desktopTheme":"LIGHT,IMAGE,#0277BD,0,1280x720","startupSelection":"1","openAndroidBrowserFromWine":"t","audioDriver":"alsa"}}'
$b64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($cfg))
& $adb shell ('run-as com.winlator sh -c "mkdir -p files/rootfs/home/xuser-1; echo ' + $b64 + ' | base64 -d > files/rootfs/home/xuser-1/.container; head -c 120 files/rootfs/home/xuser-1/.container"')
Write-Output ''
Write-Output '=== 2. 部署 hwbp5 unix ntdll ==='
& $adb push 'D:\ccz_winlator_patch\build\ntdll_wine10_hwbp5.so' /data/local/tmp/ntdll_hwbp5.so 2>&1 | Out-Null
& $adb shell 'chmod 644 /data/local/tmp/ntdll_hwbp5.so'
& $adb shell 'run-as com.winlator sh -c "cp -n files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so.orig; cp /data/local/tmp/ntdll_hwbp5.so files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so; md5sum files/rootfs/opt/wine/lib/wine/x86_64-unix/ntdll.so; echo expect: fcd4c7e3faf30a41e43f285b994d32fd"'
Write-Output '=== 3. 部署 skip-int3 i386 PE ntdll（x64 不动！） ==='
& $adb push 'D:\ccz_winlator_patch\build\skipint3\ntdll_i386_windows.dll' /data/local/tmp/ntdll_i386.dll 2>&1 | Out-Null
& $adb shell 'chmod 644 /data/local/tmp/ntdll_i386.dll'
& $adb shell 'run-as com.winlator sh -c "cp -n files/rootfs/opt/wine/lib/wine/i386-windows/ntdll.dll files/rootfs/opt/wine/lib/wine/i386-windows/ntdll.dll.orig; cp /data/local/tmp/ntdll_i386.dll files/rootfs/opt/wine/lib/wine/i386-windows/ntdll.dll; md5sum files/rootfs/opt/wine/lib/wine/i386-windows/ntdll.dll; echo expect: 857bccc54d764725ce85feebe4e7c9e5"'
Write-Output '=== 4. wfm.exe = Ekd5.exe 启动器 ==='
& $adb push 'D:\ccz\曹操传加强版2.2\Ekd5.exe' /data/local/tmp/Ekd5.exe 2>&1 | Select-String 'pushed' | Out-Null
& $adb shell 'chmod 644 /data/local/tmp/Ekd5.exe'
& $adb shell 'run-as com.winlator sh -c "mkdir -p files/rootfs/home/xuser-1/.wine/drive_c/windows; cd files/rootfs/home/xuser-1/.wine/drive_c/windows; cp -n wfm.exe wfm.exe.orig 2>/dev/null; cp /data/local/tmp/Ekd5.exe wfm.exe 2>/dev/null || echo wfm_dir_not_ready; ls -la wfm.exe 2>&1"'
Write-Output '=== 5. 鼠标 + 双击间隔 prefs ==='
$xml = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>`n<map>`n    <boolean name=`"show_shortcuts_first`" value=`"false`" />`n    <string name=`"box64_version`">0.4.4</string>`n    <boolean name=`"move_cursor_to_touchpoint`" value=`"true`" />`n</map>`n"
$xb64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($xml))
& $adb shell ('run-as com.winlator sh -c "echo ' + $xb64 + ' | base64 -d > shared_prefs/com.winlator_preferences.xml"')
Write-Output '=== 6. 清理 ==='
& $adb shell 'rm -f /data/local/tmp/ntdll_hwbp5.so /data/local/tmp/ntdll_i386.dll /data/local/tmp/Ekd5.exe'
Write-Output 'setup done - restart app next'
