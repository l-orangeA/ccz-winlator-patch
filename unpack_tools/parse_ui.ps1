[Console]::OutputEncoding = [Text.Encoding]::UTF8
$x = Get-Content 'D:\ccz_winlator_patch\unpack_tools\ui_dump.xml' -Raw -Encoding UTF8
$nodes = [regex]::Matches($x, '<node[^>]*>')
foreach ($n in $nodes) {
  $s = $n.Value
  $text = [regex]::Match($s, 'text="([^"]*)"').Groups[1].Value
  $rid  = [regex]::Match($s, 'resource-id="([^"]*)"').Groups[1].Value
  $b    = [regex]::Match($s, 'bounds="([^"]*)"').Groups[1].Value
  $clk  = [regex]::Match($s, 'clickable="([^"]*)"').Groups[1].Value
  if ($text -or ($clk -eq 'true')) {
    Write-Output ("text='{0}' id='{1}' clickable={2} bounds={3}" -f $text, $rid, $clk, $b)
  }
}
