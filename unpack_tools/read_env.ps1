# Read wine process environ via run-as, print WINE/BOX64 related lines
$adb = "D:\MuMuPlayer\nx_main\adb.exe"
$cmd = 'run-as com.winlator sh -c "cat /proc/23099/environ | tr ''\0'' ''\n'' > /data/local/tmp/env23099.txt 2>&1; wc -l /data/local/tmp/env23099.txt"'
& $adb shell $cmd
& $adb shell "grep -E 'WINE|BOX64' /data/local/tmp/env23099.txt | head -20"
