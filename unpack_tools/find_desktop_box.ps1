param([string]$png)
Add-Type -AssemblyName System.Drawing
$img = [System.Drawing.Bitmap]::FromFile($png)
Write-Output ("png size: {0}x{1}" -f $img.Width, $img.Height)
# scan center row/col for non-black bounds (wine desktop box)
$cy = [int]($img.Height/2); $cx = [int]($img.Width/2)
$xmin = -1; $xmax = -1
for ($x = 0; $x -lt $img.Width; $x++) {
  $p = $img.GetPixel($x, $cy)
  if ($p.R -gt 12 -or $p.G -gt 12 -or $p.B -gt 12) { if ($xmin -lt 0) { $xmin = $x }; $xmax = $x }
}
$ymin = -1; $ymax = -1
for ($y = 0; $y -lt $img.Height; $y++) {
  $p = $img.GetPixel($cx, $y)
  if ($p.R -gt 12 -or $p.G -gt 12 -or $p.B -gt 12) { if ($ymin -lt 0) { $ymin = $y }; $ymax = $y }
}
Write-Output ("desktop box: x[{0}..{1}] y[{2}..{3}]" -f $xmin, $xmax, $ymin, $ymax)
$w = $xmax - $xmin + 1; $h = $ymax - $ymin + 1
Write-Output ("wine desktop: {0}x{1} scale_x={2:N4} scale_y={3:N4}" -f $w, $h, ($w/1280), ($h/720))
# helper: convert desktop coords -> screen tap coords for given desktop point
function Map([int]$dx, [int]$dy) {
  $sx = [int]($xmin + $dx * ($w/1280.0))
  $sy = [int]($ymin + $dy * ($h/720.0))
  Write-Output ("desktop({0},{1}) -> screen({2},{3})" -f $dx, $dy, $sx, $sy)
}
Map 659 202   # D: row in right pane of Computer window
$img.Dispose()
