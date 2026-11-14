$a = (Get-FileHash 'D:\ccz_winlator_patch\unpack_tools\winlator11\calib1.png' -Algorithm MD5).Hash
$b = (Get-FileHash 'D:\ccz_winlator_patch\unpack_tools\winlator11\calib2.png' -Algorithm MD5).Hash
Write-Output ("calib1: " + $a)
Write-Output ("calib2: " + $b)
if ($a -eq $b) { Write-Output 'IDENTICAL - no touch visible' } else { Write-Output 'DIFFERENT - touch visible' }
