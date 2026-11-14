NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'BP_FULL' in s:
    print('already')
else:
    old = NL.join([
'        char b[160]; int n = snprintf( b, sizeof(b),',
'            "SRE_ENTRY code=%lx addr=%lx rsp=%lx' + BS + 'n",',
'            (unsigned long)rec->ExceptionCode, (unsigned long)rec->ExceptionAddress,',
'            (unsigned long)RSP_sig(sigcontext) );'])
    new = NL.join([
'        char b[224]; int n = snprintf( b, sizeof(b),',
'            "SRE_ENTRY code=%lx addr=%lx rsp=%lx ef=%lx cs=%lx np=%lx info0=%lx thesp=%lx' + BS + 'n",',
'            (unsigned long)rec->ExceptionCode, (unsigned long)rec->ExceptionAddress,',
'            (unsigned long)RSP_sig(sigcontext), (unsigned long)EFL_sig(sigcontext),',
'            (unsigned long)CS_sig(sigcontext), (unsigned long)rec->NumberParameters,',
'            rec->NumberParameters ? (unsigned long)rec->ExceptionInformation[0] : 0,',
'            (unsigned long)context->Rsp );'])
    assert old in s, 'sre entry'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched BP_FULL')
