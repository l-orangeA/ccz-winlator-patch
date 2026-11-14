$adb = "D:\MuMuPlayer\nx_main\adb.exe"
# 1. 移走附属文件到暂存区（SV 已备份）
& $adb shell 'mkdir -p /sdcard/ccz_stash'
$extras = @('/sdcard/ccz/2.10随机工具-3.2.2.exe', '/sdcard/ccz/waitwin.exe', '/sdcard/ccz/random_rules.json',
            '/sdcard/ccz/randomizer_settings.json', '/sdcard/ccz/tr_tail.bin', '/sdcard/ccz/uris.txt',
            '/sdcard/ccz/Ekd5.exe-up.txt', '/sdcard/ccz/新建文件夹', '/sdcard/ccz/脱壳进度日志.md')
foreach ($f in $extras) {
  $out = & $adb shell ('mv "' + $f + '" /sdcard/ccz_stash/ 2>&1')
  if ($out) { Write-Output ("mv " + $f + " -> " + $out) }
}
# 2. 验证 ccz 只剩核心游戏文件，然后删除
$remaining = (& $adb shell 'ls /sdcard/ccz | wc -l').Trim()
Write-Output ("remaining before rm: " + $remaining)
& $adb shell 'rm -rf /sdcard/ccz'
& $adb shell 'mkdir -p /sdcard/ccz'
Write-Output 'ccz wiped and recreated'
# 3. 从 PC 干净源 push（push 到暂存名再合并，避免嵌套目录）
& $adb push 'D:\ccz\曹操传加强版2.2' /sdcard/ccz_tmp
