NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/signal_i386.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'BPCTX' in s:
    print('already')
else:
    old = NL.join([
'    TRACE( "code=%lx flags=%lx addr=%p ip=%08lx' + BS + 'n",',
'           rec->ExceptionCode, rec->ExceptionFlags, rec->ExceptionAddress, context->Eip );'])
    new = NL.join([
'    TRACE( "code=%lx flags=%lx addr=%p ip=%08lx' + BS + 'n",',
'           rec->ExceptionCode, rec->ExceptionFlags, rec->ExceptionAddress, context->Eip );',
'    if (rec->ExceptionCode == 0x80000003 || rec->ExceptionCode == 0x80000004 || rec->ExceptionCode == 0xc0000005)',
'    {',
'        char bb[160]; int bn = snprintf( bb, sizeof(bb),',
'            "BPCTX code=%lx excaddr=%lx eip=%lx esp=%lx eflags=%lx' + BS + 'n",',
'            (unsigned long)rec->ExceptionCode, (unsigned long)rec->ExceptionAddress,',
'            (unsigned long)context->Eip, (unsigned long)context->Esp,',
'            (unsigned long)context->EFlags );',
'        write( 2, bb, bn );',
'    }'])
    assert old in s, 'dispatch trace'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched BPCTX')
