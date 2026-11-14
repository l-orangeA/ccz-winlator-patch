$h = (Get-FileHash 'D:\ccz_winlator_patch\apk\winlator8.0_ccz_fr60_glpin.apk' -Algorithm MD5).Hash.ToLower()
Write-Output ("APK md5: " + $h)
Write-Output "expect : a923fd6e6ff5dd0c45af69005fc7a42e"
if ($h -eq 'a923fd6e6ff5dd0c45af69005fc7a42e') { Write-Output 'APK_MATCH' } else { Write-Output 'APK_MISMATCH' }
$files = @('D:\ccz_winlator_patch\build\ntdll_fix110b.so','D:\ccz_winlator_patch\build\win32u_r109g.so','D:\ccz_winlator_patch\build\wineserver_r112b.so')
foreach ($f in $files) {
  $m = (Get-FileHash $f -Algorithm MD5).Hash.ToLower()
  Write-Output ((Split-Path $f -Leaf) + ' = ' + $m)
}
