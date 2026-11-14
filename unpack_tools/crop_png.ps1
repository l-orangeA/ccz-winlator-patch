param([string]$png, [string]$out, [int]$x, [int]$y, [int]$w, [int]$h)
Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Bitmap]::FromFile($png)
$rect = New-Object System.Drawing.Rectangle($x, $y, $w, $h)
$crop = $img.Clone($rect, $img.PixelFormat)
$crop.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$img.Dispose(); $crop.Dispose()
Write-Output "saved $out ($w x $h at $x,$y)"
