NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'WCTX' in s:
    print('already')
else:
    old = NL.join([
'    if (size != sizeof(I386_CONTEXT)) return STATUS_INFO_LENGTH_MISMATCH;'])
    new = NL.join([
'    if (size != sizeof(I386_CONTEXT)) return STATUS_INFO_LENGTH_MISMATCH;',
'    {',
'        static int wcn;',
'        if (wcn++ < 200 && (context->ContextFlags & 1))',
'        {',
'            char wb[160]; int wn = snprintf( wb, sizeof(wb),',
'                "WCTX eip=%lx esp=%lx ef=%lx flags=%lx' + BS + 'n",',
'                (unsigned long)context->Eip, (unsigned long)context->Esp,',
'                (unsigned long)context->EFlags, (unsigned long)context->ContextFlags );',
'            write( 2, wb, wn );',
'        }',
'    }'])
    assert old in s, 'wow64 ctx'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched WCTX')
