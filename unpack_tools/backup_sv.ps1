$adb = "D:\MuMuPlayer\nx_main\adb.exe"
& $adb shell 'rm -rf /sdcard/SV_backup_20261108'
& $adb shell 'cp -r /sdcard/ccz/SV /sdcard/SV_backup_20261108 && echo copy_ok'
$a = (& $adb shell 'ls /sdcard/ccz/SV | wc -l').Trim()
$b = (& $adb shell 'ls /sdcard/SV_backup_20261108 | wc -l').Trim()
$sa = (& $adb shell 'du -sk /sdcard/ccz/SV').Trim()
$sb = (& $adb shell 'du -sk /sdcard/SV_backup_20261108').Trim()
Write-Output ("orig files=$a backup files=$b")
Write-Output ("orig KB=$sa backup KB=$sb")
if ($a -eq $b) { Write-Output 'SV_BACKUP_MATCH' } else { Write-Output 'SV_BACKUP_MISMATCH - DO NOT DELETE' }
