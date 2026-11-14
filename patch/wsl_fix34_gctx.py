NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'GCTX_DBG' in s:
    print('already')
else:
    old = NL.join([
'NTSTATUS get_thread_wow64_context( HANDLE handle, void *ctx, ULONG size )',
'{',
'    DWORD needed_flags;'])
    new = NL.join([
'NTSTATUS get_thread_wow64_context( HANDLE handle, void *ctx, ULONG size )',
'{',
'    DWORD needed_flags;',
'    { I386_CONTEXT *c0 = ctx; char b[128]; int n = snprintf( b, sizeof(b),',
'        "GCTX_DBG size=%lx flags=%lx eip=%lx' + BS + 'n", (unsigned long)size,',
'        (unsigned long)c0->ContextFlags, (unsigned long)c0->Eip ); write( 2, b, n ); }'])
    assert old in s
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched GCTX_DBG')
