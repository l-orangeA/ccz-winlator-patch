$adb = "D:\MuMuPlayer\nx_main\adb.exe"

# 1. 终态容器配置（保留这台的图形/音频/drives，注入壳适配 env + COMPATIBILITY 预设 + 改名 ccz）
$cfg = '{"id":1,"name":"ccz","screenSize":"1280x720","envVars":"ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB mesa_glthread=true WINEESYNC=1 TU_DEBUG=noconform LC_ALL=zh_CN.utf8 WINEDEBUG=-all BOX64_DYNAREC_BIGBLOCK=3 BOX64_DYNAREC_FORWARD=1024 BOX64_DYNAREC_CALLRET=1 WINE_SS=0xe00 WINE_HWBP_SOFT=1","cpuList":"0,1,2,3","cpuListWoW64":"4,5,6,7","graphicsDriver":"turnip,gladio","dxwrapper":"dxvk","graphicsDriverConfig":"version=26.2.0,maxDeviceMemory=0,directRendering=1,presentMode=1|","audioDriver":"alsa","wincomponents":"direct3d=1,directsound=1,directmusic=1,directshow=0,directplay=0,xaudio=1,vcrun2005=0,vcrun2010=1,wmdecoder=1","drives":"D:/storage/emulated/0/ccz","hudMode":0,"startupSelection":1,"box64Preset":"COMPATIBILITY","desktopTheme":"LIGHT,IMAGE,#0277BD,0","extraData":{"appVersion":"33","rfsVersion":"23","userRegLastModified":"1794143907000","dxwrapper":"dxvk-2.4.1-vkd3d-2.14.1-wined3d","wincomponents":"direct3d=1,directsound=1,directmusic=1,directshow=0,directplay=0,xaudio=1,vcrun2005=0,vcrun2010=1,wmdecoder=1","desktopTheme":"LIGHT,IMAGE,#0277BD,0,1280x720","startupSelection":"1","openAndroidBrowserFromWine":"t","audioDriver":"alsa"}}'
$b64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($cfg))

Write-Output '=== 1. 停 app ==='
& $adb shell 'am force-stop com.winlator; sleep 1'
Write-Output '=== 2. 写入终态 .container ==='
& $adb shell ('run-as com.winlator sh -c "echo ' + $b64 + ' | base64 -d > files/rootfs/home/xuser-1/.container; head -c 200 files/rootfs/home/xuser-1/.container"')
Write-Output ''
Write-Output '=== 3. 追加注册表 Run 键 + 改 win.ini（这台 D: 根=ccz） ==='
& $adb shell 'run-as com.winlator sh -c "echo W1NvZnR3YXJlXFxNaWNyb3NvZnRcXFdpbmRvd3NcXEN1cnJlbnRWZXJzaW9uXFxSdW5dCiJFa2Q1VGVzdCI9IkQ6XFxla2Q1X3Rlc3QuYmF0Igo= | base64 -d >> files/rootfs/home/xuser-1/.wine/user.reg; tail -2 files/rootfs/home/xuser-1/.wine/user.reg"'
& $adb shell 'run-as com.winlator sh -c "echo W3dpbmRvd3NdDQpydW49RDpcZWtkNV90ZXN0LmJhdA0KDQpbbWFpbF0NCm1hcGk9MQ0K | base64 -d > files/rootfs/home/xuser-1/.wine/drive_c/windows/win.ini; cat files/rootfs/home/xuser-1/.wine/drive_c/windows/win.ini"'
Write-Output '=== 4. 双击间隔放宽到 2000ms ==='
& $adb shell 'run-as com.winlator sh -c "echo W0NvbnRyb2wgUGFuZWxcXERlc2t0b3BdDQoiRG91YmxlQ2xpY2tTcGVlZCI9IjIwMDAiCg== | base64 -d >> files/rootfs/home/xuser-1/.wine/user.reg; tail -2 files/rootfs/home/xuser-1/.wine/user.reg"'
Write-Output '=== 5. 完成待重启 ==='
