$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 备份新设备 SV ==='
& $adb shell 'rm -rf /sdcard/SV_backup_k70; cp -r /sdcard/ccz/SV /sdcard/SV_backup_k70 && echo backup_ok'
$a = (& $adb shell 'ls /sdcard/ccz/SV | wc -l').Trim()
$b = (& $adb shell 'ls /sdcard/SV_backup_k70 | wc -l').Trim()
Write-Output ("orig=$a backup=$b")
Write-Output '=== 2. 推 PC v2.20 干净源（覆盖） ==='
& $adb push 'D:\ccz\曹操传加强版2.2' /sdcard/ccz_v20 2>&1 | Select-String 'pushed'
Write-Output '=== 3. 合并（内容级，避免嵌套） ==='
& $adb shell 'cp -a /sdcard/ccz_v20/. /sdcard/ccz/ && echo merge_ok'
Write-Output '=== 4. 恢复新设备原 SV 独有档 ==='
& $adb shell 'cd /sdcard/SV_backup_k70 && for f in *; do [ -e /sdcard/ccz/SV/$f ] || { cp -a "$f" /sdcard/ccz/SV/; echo "device_only_added: $f"; }; done; true'
Write-Output '=== 5. 核对 ==='
$sv = (& $adb shell 'ls /sdcard/ccz/SV | wc -l').Trim()
Write-Output ("SV total=" + $sv)
& $adb shell 'md5sum /sdcard/ccz/Ekd5.exe'
