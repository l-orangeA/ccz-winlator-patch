$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 正确合并（内容级拷贝） ==='
& $adb shell 'cp -a /sdcard/ccz_src/. /sdcard/ccz/ && echo merge_ok'
Write-Output '=== 2. 设备 SV 456 覆盖 + PC 独有补入 ==='
& $adb shell 'cp -a /sdcard/SV_backup_20261108/. /sdcard/ccz/SV/ && echo sv_device_ok'
& $adb shell 'cd /sdcard/ccz_src/SV && for f in *; do [ -e /sdcard/SV_backup_20261108/$f ] || { cp -a "$f" /sdcard/ccz/SV/; echo "pc_only_added: $f"; }; done; true'
Write-Output '=== 3. 附属文件移回 ==='
& $adb shell 'mv /sdcard/ccz_stash/* /sdcard/ccz/ && echo extras_ok'
Write-Output '=== 4. 校验 ==='
$sv = (& $adb shell 'ls /sdcard/ccz/SV | wc -l').Trim()
$top = (& $adb shell 'ls /sdcard/ccz | wc -l').Trim()
$total = (& $adb shell 'find /sdcard/ccz -type f | wc -l').Trim()
Write-Output ("SV=" + $sv + "  top=" + $top + "  total=" + $total)
Write-Output ("Ekd5.exe: " + (& $adb shell 'md5sum /sdcard/ccz/Ekd5.exe'))
Write-Output ("zlib.dll: " + (& $adb shell 'md5sum /sdcard/ccz/zlib.dll'))
Write-Output ("AutoDLL.ini: " + (& $adb shell 'md5sum /sdcard/ccz/AutoDLL.ini'))
Write-Output '=== 5. 清理 ==='
& $adb shell 'rm -rf /sdcard/ccz_src /sdcard/ccz_stash && echo cleaned'
& $adb shell 'ls /sdcard/ccz | head -35'
