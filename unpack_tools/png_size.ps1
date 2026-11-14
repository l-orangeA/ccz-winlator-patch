param([string]$png)
Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Bitmap]::FromFile($png)
Write-Output ("size: {0}x{1}" -f $img.Width, $img.Height)
$img.Dispose()
