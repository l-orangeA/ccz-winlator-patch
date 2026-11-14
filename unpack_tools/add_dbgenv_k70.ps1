$adb = "D:\MuMuPlayer\nx_main\adb.exe"
& $adb shell 'am force-stop com.winlator; sleep 1'
$cfg = '{"id":1,"name":"ccz","screenSize":"1280x720","envVars":"ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB mesa_glthread=true WINEESYNC=1 TU_DEBUG=noconform LC_ALL=zh_CN.utf8 WINEDEBUG=+loaddll,+seh WINE_DBG_LOG=1 BOX64_DYNAREC_BIGBLOCK=3 BOX64_DYNAREC_FORWARD=1024 BOX64_DYNAREC_CALLRET=1 WINE_SS=0xe00 WINE_HWBP_SOFT=1","cpuList":"0,1,2,3","cpuListWoW64":"4,5,6,7","graphicsDriver":"turnip,gladio","dxwrapper":"dxvk","audioDriver":"alsa","wincomponents":"direct3d=1,directsound=1,directmusic=1,directshow=0,directplay=0,xaudio=1,vcrun2005=0,vcrun2010=1,wmdecoder=1","drives":"D:/storage/emulated/0/ccz","hudMode":0,"startupSelection":1,"box64Preset":"COMPATIBILITY","desktopTheme":"LIGHT,IMAGE,#0277BD,0","extraData":{"appVersion":"33","rfsVersion":"23","dxwrapper":"dxvk-2.4.1-vkd3d-2.14.1-wined3d","wincomponents":"direct3d=1,directsound=1,directmusic=1,directshow=0,directplay=0,xaudio=1,vcrun2005=0,vcrun2010=1,wmdecoder=1","desktopTheme":"LIGHT,IMAGE,#0277BD,0,1280x720","startupSelection":"1","openAndroidBrowserFromWine":"t","audioDriver":"alsa"}}'
$b64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($cfg))
& $adb shell ('run-as com.winlator sh -c "echo ' + $b64 + ' | base64 -d > files/rootfs/home/xuser-1/.container; grep -o \"WINEDEBUG=[^\\\"]*\" files/rootfs/home/xuser-1/.container; grep -c WINE_DBG_LOG files/rootfs/home/xuser-1/.container"')
Write-Output '=== wfm 确认仍是 Ekd5 ==='
& $adb shell 'run-as com.winlator ls -la files/rootfs/home/xuser-1/.wine/drive_c/windows/wfm.exe'
Write-Output '=== 重启 app ==='
& $adb shell 'monkey -p com.winlator -c android.intent.category.LAUNCHER 1' 2>&1 | Out-Null
Write-Output 'restarted - 请再点 ▶'
