$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== 1. 救出附属文件 ==='
& $adb shell 'rm -rf /sdcard/ccz_stash; mkdir -p /sdcard/ccz_stash'
$extras = @('2.10随机工具-3.2.2.exe','waitwin.exe','random_rules.json','randomizer_settings.json','tr_tail.bin','uris.txt','Ekd5.exe-up.txt','新建文件夹','脱壳进度日志.md')
foreach ($f in $extras) {
  # 用 mv -f 逐个移动，失败忽略（可能不存在）
  & $adb shell ('mv -f "/sdcard/ccz/' + $f + '" /sdcard/ccz_stash/ 2>/dev/null; true') | Out-Null
}
$n = (& $adb shell 'ls /sdcard/ccz_stash | wc -l').Trim()
Write-Output ("stashed extras: " + $n)
Write-Output '=== 2. 清空 ccz ==='
& $adb shell 'rm -rf /sdcard/ccz && mkdir -p /sdcard/ccz && echo wiped'
Write-Output '=== 3. 检查已 push 的源结构 ==='
$L = (& $adb shell 'ls /sdcard/ccz_core_tmp | head -8')
Write-Output $L
