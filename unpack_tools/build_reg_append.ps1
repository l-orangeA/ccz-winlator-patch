$reg = "[Software\\Microsoft\\Windows\\CurrentVersion\\Run]`n`"Ekd5Test`"=`"D:\\ccz\\ekd5_test.bat`"`n"
$b64 = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($reg))
Write-Output $b64
Write-Output '--- decoded check ---'
[System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))
