$adb = "D:\MuMuPlayer\nx_main\adb.exe"
$ws = (& $adb shell 'pidof wineserver').Trim()
Write-Output ("wineserver pid: " + $ws)
& $adb shell ('run-as com.winlator sh -c "cat /proc/' + $ws + '/environ > files/envcheck.txt 2>&1; grep -c . files/envcheck.txt"')
& $adb exec-out run-as com.winlator cat files/envcheck.txt > "D:\ccz_winlator_patch\unpack_tools\winlator11\env_newdev.txt"
$lines = [System.IO.File]::ReadAllText('D:\ccz_winlator_patch\unpack_tools\winlator11\env_newdev.txt') -split "`0"
Write-Output ("total vars: " + $lines.Count)
Write-Output '--- WINE_SS / HWBP / WINEDEBUG / BOX64_DYNAREC ---'
$lines | Where-Object { $_ -match 'WINE_SS|HWBP|WINEDEBUG|BOX64_DYNAREC|LC_ALL' } | ForEach-Object { Write-Output ("  [" + $_ + "]") }
Write-Output '--- explorer/exe processes ---'
& $adb shell 'ps -A 2>/dev/null | grep -E "explorer|wine|box64" | head -8'
