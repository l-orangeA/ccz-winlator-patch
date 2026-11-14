$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 合并 PC 源（通配符引用） ==='
& $adb shell 'cp -a /sdcard/ccz_core_tmp/*/. /sdcard/ccz/ && echo merge_ok'
Write-Output '=== 设备 SV 456 覆盖同名 ==='
& $adb shell 'cp -a /sdcard/SV_backup_20261108/. /sdcard/ccz/SV/ && echo sv_ok'
Write-Output '=== 校验 ==='
$sv = (& $adb shell 'ls /sdcard/ccz/SV | wc -l').Trim()
$top = (& $adb shell 'ls /sdcard/ccz | wc -l').Trim()
$total = (& $adb shell 'find /sdcard/ccz -type f | wc -l').Trim()
Write-Output ("SV=" + $sv + "  top=" + $top + "  total=" + $total)
Write-Output ("Ekd5.exe: " + (& $adb shell 'md5sum /sdcard/ccz/Ekd5.exe'))
Write-Output ("Ekd5.exe size: " + (& $adb shell 'ls -la /sdcard/ccz/Ekd5.exe'))
Write-Output ("zlib.dll: " + (& $adb shell 'md5sum /sdcard/ccz/zlib.dll'))
Write-Output '=== 清理暂存 ==='
& $adb shell 'rm -rf /sdcard/ccz_core_tmp'
& $adb shell 'ls /sdcard/ccz | head -30'
