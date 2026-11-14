NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'DRPROBE' in s:
    print('already')
else:
    old = NL.join([
'            (unsigned long)context->Rsp );',
'        write( 2, b, n );'])
    new = NL.join([
'            (unsigned long)context->Rsp );',
'        write( 2, b, n );',
'        n = snprintf( b, sizeof(b),',
'            "DRPROBE dr0=%lx dr1=%lx dr2=%lx dr3=%lx dr6=%lx dr7=%lx eax=%lx ecx=%lx' + BS + 'n",',
'            (unsigned long)context->Dr0, (unsigned long)context->Dr1,',
'            (unsigned long)context->Dr2, (unsigned long)context->Dr3,',
'            (unsigned long)context->Dr6, (unsigned long)context->Dr7,',
'            (unsigned long)context->Rax, (unsigned long)context->Rcx );',
'        write( 2, b, n );'])
    assert old in s, 'sre tail'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched DRPROBE')
