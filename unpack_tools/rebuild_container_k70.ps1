$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 停 app ==='
& $adb shell 'am force-stop com.winlator; sleep 1'
Write-Output '=== 2. 重建 home/xuser-1：tzst 模板骨架 ==='
& $adb shell 'run-as com.winlator sh -c "mkdir -p files/rootfs/home/xuser-1 && cd files/rootfs/home/xuser-1 && tar -xf /data/local/tmp/pattern.tar && ls -la && cat .wine/.update-timestamp"'
Write-Output '=== 3. 写终态 .container ==='
$cfg = '{"id":1,"name":"ccz","screenSize":"1280x720","envVars":"ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB mesa_glthread=true WINEESYNC=1 TU_DEBUG=noconform LC_ALL=zh_CN.utf8 WINEDEBUG=-all BOX64_DYNAREC_BIGBLOCK=3 BOX64_DYNAREC_FORWARD=1024 BOX64_DYNAREC_CALLRET=1 WINE_SS=0xe00 WINE_HWBP_SOFT=1","cpuList":"0,1,2,3","cpuListWoW64":"4,5,6,7","graphicsDriver":"turnip,gladio","dxwrapper":"dxvk","audioDriver":"alsa","wincomponents":"direct3d=1,directsound=1,directmusic=1,directshow=0,directplay=0,xaudio=1,vcrun2005=0,vcrun2010=1,wmdecoder=1","drives":"D:/storage/emulated/0/ccz","hudMode":0,"startupSelection":1,"box64Preset":"COMPATIBILITY","desktopTheme":"LIGHT,IMAGE,#0277BD,0","extraData":{"appVersion":"33","rfsVersion":"23","dxwrapper":"dxvk-2.4.1-vkd3d-2.14.1-wined3d","wincomponents":"direct3d=1,directsound=1,directmusic=1,directshow=0,directplay=0,xaudio=1,vcrun2005=0,vcrun2010=1,wmdecoder=1","desktopTheme":"LIGHT,IMAGE,#0277BD,0,1280x720","startupSelection":"1","openAndroidBrowserFromWine":"t","audioDriver":"alsa"}}'
$b64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($cfg))
& $adb shell ('run-as com.winlator sh -c "echo ' + $b64 + ' | base64 -d > files/rootfs/home/xuser-1/.container; wc -c files/rootfs/home/xuser-1/.container"')
Write-Output '=== 4. wfm.exe = Ekd5 ==='
& $adb push 'D:\ccz\曹操传加强版2.2\Ekd5.exe' /data/local/tmp/Ekd5.exe 2>&1 | Out-Null
& $adb shell 'chmod 644 /data/local/tmp/Ekd5.exe'
& $adb shell 'run-as com.winlator sh -c "cp /data/local/tmp/Ekd5.exe files/rootfs/home/xuser-1/.wine/drive_c/windows/wfm.exe; ls -la files/rootfs/home/xuser-1/.wine/drive_c/windows/wfm.exe"'
Write-Output '=== 5. user.reg：Run 键 + 双击间隔 ==='
& $adb shell 'run-as com.winlator sh -c "echo W1NvZnR3YXJlXFxNaWNyb3NvZnRcXFdpbmRvd3NcXEN1cnJlbnRWZXJzaW9uXFxSdW5dCiJFa2Q1VGVzdCI9IkQ6XFxla2Q1X3Rlc3QuYmF0Igo= | base64 -d >> files/rootfs/home/xuser-1/.wine/user.reg; echo W0NvbnRyb2wgUGFuZWxcXERlc2t0b3BdDQoiRG91YmxlQ2xpY2tTcGVlZCI9IjIwMDAiCg== | base64 -d >> files/rootfs/home/xuser-1/.wine/user.reg; tail -4 files/rootfs/home/xuser-1/.wine/user.reg"'
Write-Output '=== 6. 重启 app ==='
& $adb shell 'monkey -p com.winlator -c android.intent.category.LAUNCHER 1' 2>&1 | Out-Null
Start-Sleep -Seconds 6
& $adb shell 'screencap -p /sdcard/probe_shot.png'
& $adb pull /sdcard/probe_shot.png 'D:\ccz_winlator_patch\unpack_tools\winlator11\k70_list.png' | Out-Null
Write-Output 'rebuild done'
