$adb = "D:\MuMuPlayer\nx_main\adb.exe"
# push 落在 /sdcard/ccz_tmp/曹操传加强版2.2/ 下，先把内容并到 /sdcard/ccz
Write-Output '=== 合并 PC 源到 ccz ==='
& $adb shell 'cp -a /sdcard/ccz_tmp/曹操传加强版2.2/. /sdcard/ccz/ && echo merge_ok'
# 设备最新存档覆盖（备份 SV 456 为准）
Write-Output '=== 设备 SV 覆盖 ==='
& $adb shell 'cp -a /sdcard/SV_backup_20261108/. /sdcard/ccz/SV/ && echo sv_restore_ok'
# PC 独有存档补入（不覆盖已有）
Write-Output '=== PC 独有存档补入 ==='
& $adb shell 'cd /sdcard/ccz_tmp/曹操传加强版2.2/SV && for f in *; do [ -e /sdcard/SV_backup_20261108/$f ] || { cp -a $f /sdcard/ccz/SV/; echo "pc_only: $f"; }; done; true'
# 附属文件移回
Write-Output '=== 附属文件移回 ==='
& $adb shell 'ls /sdcard/ccz_stash'
& $adb shell 'mv /sdcard/ccz_stash/* /sdcard/ccz/ 2>&1; true'
# 校验
Write-Output '=== 校验 ==='
$sv = (& $adb shell 'ls /sdcard/ccz/SV | wc -l').Trim()
$total = (& $adb shell 'find /sdcard/ccz -type f | wc -l').Trim()
$md5 = (& $adb shell 'md5sum /sdcard/ccz/Ekd5.exe').Trim()
Write-Output ("SV files=" + $sv + "  total files=" + $total)
Write-Output ("Ekd5.exe md5: " + $md5)
& $adb shell 'rm -rf /sdcard/ccz_tmp /sdcard/ccz_stash'
Write-Output 'stash cleaned'
