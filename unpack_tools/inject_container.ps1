$adb = "D:\MuMuPlayer\nx_main\adb.exe"
# 手工注入容器记录：files/containers/Container-1/.container
$cfg = '{"id":1,"name":"ccz","screenSize":"1280x720","envVars":"ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB MESA_VK_WSI_PRESENT_MODE=mailbox MESA_VK_WSI_DEBUG=(sw mesa_glthread=true WINEESYNC=1 DXVK_HUD=fps,gpuload,version,api TU_DEBUG=sysmem,noconform LC_ALL=zh_CN.utf8 WINEDEBUG=-all BOX64_DYNAREC_BIGBLOCK=3 BOX64_DYNAREC_FORWARD=1024 BOX64_DYNAREC_CALLRET=1 WINE_SS=0xe00 WINE_HWBP_SOFT=1","cpuList":"0,1,2,3","cpuListWoW64":"4,5,6,7","graphicsDriver":"vortek,gladio","dxwrapper":"dxvk","audioDriver":"alsa","wincomponents":"direct3d=1,directsound=1,directmusic=1,directshow=0,directplay=0,xaudio=1,vcrun2005=0,vcrun2010=1,wmdecoder=1","drives":"D:/storage/emulated/0","hudMode":1,"startupSelection":1,"box64Preset":"COMPATIBILITY","desktopTheme":"LIGHT,IMAGE,#0277BD,0","extraData":{"appVersion":"33","rfsVersion":"23","dxwrapper":"dxvk-1.10.3-vkd3d-2.14.1-wined3d","wincomponents":"direct3d=1,directsound=1,directmusic=1,directshow=0,directplay=0,xaudio=1,vcrun2005=0,vcrun2010=1,wmdecoder=1","desktopTheme":"LIGHT,IMAGE,#0277BD,0,1280x720","startupSelection":"1","openAndroidBrowserFromWine":"t","audioDriver":"alsa"}}'
$bytes = [System.Text.Encoding]::UTF8.GetBytes($cfg)
$b64 = [Convert]::ToBase64String($bytes)
& $adb shell 'am force-stop com.winlator; sleep 1'
& $adb shell ('run-as com.winlator sh -c "mkdir -p files/containers/Container-1; echo ' + $b64 + ' | base64 -d > files/containers/Container-1/.container; ls -la files/containers/Container-1/; head -c 120 files/containers/Container-1/.container"')
& $adb shell 'monkey -p com.winlator -c android.intent.category.LAUNCHER 1' | Out-Null
Start-Sleep -Seconds 5
& $adb shell 'screencap -p /sdcard/probe_shot.png'
& $adb pull /sdcard/probe_shot.png 'D:\ccz_winlator_patch\unpack_tools\winlator11\newdev8.png' | Out-Null
Write-Output 'done'
