import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
old = '''    if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000)
    {
        char b[192]; int n = snprintf( b, sizeof(b),
            "SEGV_ENTRY rip=%lx addr=%lx err=%x rsp=%lx trap=%lx\\n",'''
new = '''    {
        char b[192]; int n = snprintf( b, sizeof(b),
            "SEGV_ALL sig=%d code=%d rip=%lx addr=%lx err=%x rsp=%lx trap=%lx\\n",
            signal, siginfo->si_code,
            (unsigned long)RIP_sig(ucontext), (unsigned long)siginfo->si_addr,
            (unsigned)(ERROR_sig(ucontext)), (unsigned long)RSP_sig(ucontext),
            (unsigned long)TRAP_sig(ucontext) );
        write( 2, b, n );
    }
    if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000)
    {
        char b[192]; int n = snprintf( b, sizeof(b),
            "SEGV_ENTRY rip=%lx addr=%lx err=%x rsp=%lx trap=%lx\\n",'''
assert old in s, 'segv entry'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('segv-all added')
