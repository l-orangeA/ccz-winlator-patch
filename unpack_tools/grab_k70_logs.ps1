$adb = "D:\MuMuPlayer\nx_main\adb.exe"
Write-Output '=== wine_dbg 文件列表 ==='
& $adb shell 'run-as com.winlator ls -la files/rootfs/home/xuser-1/wine_dbg_*.log 2>&1'
Write-Output '=== 每个文件的行数和头尾 ==='
$files = & $adb shell 'run-as com.winlator sh -c "ls files/rootfs/home/xuser-1/wine_dbg_*.log 2>/dev/null"'
foreach ($f in $files) {
  $f = $f.Trim()
  if (-not $f) { continue }
  $name = Split-Path $f -Leaf
  $local = "D:\ccz_winlator_patch\unpack_tools\winlator11\k70_$name"
  & $adb exec-out run-as com.winlator cat $f > $local
  $size = (Get-Item $local).Length
  Write-Output ("$name -> $size bytes")
}
Write-Output '=== ekd5_run.log ==='
& $adb shell 'cat /sdcard/ccz/ekd5_run.log 2>&1'
