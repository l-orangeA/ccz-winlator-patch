[Console]::OutputEncoding = [Text.Encoding]::UTF8
$t = Get-Content 'D:\ccz_winlator_patch\unpack_tools\winlator11\dbg_13512.log' -Encoding UTF8
$i = 0
foreach ($line in $t) {
  $i++
  if ($line -match 'dispatch_exception|call_seh_handlers|handler|RtlUnwindEx code|RtlRestoreContext returning|syscall_fault|virtual_setup|TerminateProcess|starting|Loaded L"C:\\windows\\[a-z]+32?\.dll|ntdll') {
    Write-Output ("{0,4}: {1}" -f $i, $line)
  }
}
