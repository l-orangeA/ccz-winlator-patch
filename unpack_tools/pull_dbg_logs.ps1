$adb = "D:\MuMuPlayer\nx_main\adb.exe"
foreach ($pid_ in @('13479','13512','13699')) {
  $local = "D:\ccz_winlator_patch\unpack_tools\winlator11\dbg_$pid_.log"
  & $adb exec-out run-as com.winlator cat "files/rootfs/home/xuser-1/wine_dbg_$pid_.log" > $local
  $size = (Get-Item $local).Length
  Write-Output ("wine_dbg_" + $pid_ + ".log -> " + $size + " bytes")
}
