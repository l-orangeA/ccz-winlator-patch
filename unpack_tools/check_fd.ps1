$adb = "D:\MuMuPlayer\nx_main\adb.exe"
$pids = (& $adb shell 'pidof wineserver winedevice.exe') -split '\s+' | Where-Object { $_ }
foreach ($p in $pids) {
  Write-Output ("pid $p fd2: " + (& $adb shell "run-as com.winlator ls -la /proc/$p/fd/2 2>&1"))
  Write-Output ("pid $p fd1: " + (& $adb shell "run-as com.winlator ls -la /proc/$p/fd/1 2>&1"))
}
