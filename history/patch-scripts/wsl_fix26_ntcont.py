NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/server.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'NTCONT' in s:
    print('already')
else:
    old = '    return signal_set_full_context( context );'
    new = NL.join([
'    if ((ULONG_PTR)context->Rip < 0x10000000)',
'    {',
'        char b[160]; int n = snprintf( b, sizeof(b),',
'            "NTCONT rip=%lx eflags=%lx tf=%d eax=%lx ecx=%lx edx=%lx' + BS + 'n",',
'            (unsigned long)context->Rip, (unsigned long)context->EFlags,',
'            (context->EFlags & 0x100) ? 1 : 0,',
'            (unsigned long)context->Rax, (unsigned long)context->Rcx,',
'            (unsigned long)context->Rdx );',
'        write( 2, b, n );',
'    }',
'    return signal_set_full_context( context );'])
    assert old in s, 'ntcontinue'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched NTCONT')
