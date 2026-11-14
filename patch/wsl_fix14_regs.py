NL = chr(10)
BS = chr(92)

p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()

# decimal dump instead of hex (hex display via box64 trace mangles nibbles)
old = NL.join([
'                    n = snprintf( b, sizeof(b),',
'                        "STKDUMPMEM %lx: %08lx %08lx %08lx %08lx' + BS + 'n",',
'                        (unsigned long)(a + i),',
'                        (unsigned long)*(DWORD *)(a + i),',
'                        (unsigned long)*(DWORD *)(a + i + 4),',
'                        (unsigned long)*(DWORD *)(a + i + 8),',
'                        (unsigned long)*(DWORD *)(a + i + 12) );'])
new = NL.join([
'                    n = snprintf( b, sizeof(b),',
'                        "STKDEC %lu: %lu %lu %lu %lu' + BS + 'n",',
'                        (unsigned long)(a + i),',
'                        (unsigned long)*(DWORD *)(a + i),',
'                        (unsigned long)*(DWORD *)(a + i + 4),',
'                        (unsigned long)*(DWORD *)(a + i + 8),',
'                        (unsigned long)*(DWORD *)(a + i + 12) );'])
assert old in s, 'dump fmt'
s = s.replace(old, new, 1)

# register dump for 32-bit page faults
old = NL.join([
'                "PF_ENTRY rip=%lx addr=%lx err=%x rsp=%lx trap=%lx' + BS + 'n",',
'                (unsigned long)RIP_sig(ucontext), (unsigned long)siginfo->si_addr,',
'                (unsigned)(ERROR_sig(ucontext)), (unsigned long)RSP_sig(ucontext),',
'                (unsigned long)TRAP_sig(ucontext) );',
'            write( 2, b, n );'])
new = NL.join([
'                "PF_ENTRY rip=%lx addr=%lx err=%x rsp=%lx trap=%lx' + BS + 'n",',
'                (unsigned long)RIP_sig(ucontext), (unsigned long)siginfo->si_addr,',
'                (unsigned)(ERROR_sig(ucontext)), (unsigned long)RSP_sig(ucontext),',
'                (unsigned long)TRAP_sig(ucontext) );',
'            write( 2, b, n );',
'            n = snprintf( b, sizeof(b),',
'                "PFREGS rax=%lu rbx=%lu rcx=%lu rdx=%lu rsi=%lu rdi=%lu rbp=%lu' + BS + 'n",',
'                (unsigned long)RAX_sig(ucontext), (unsigned long)RBX_sig(ucontext),',
'                (unsigned long)RCX_sig(ucontext), (unsigned long)RDX_sig(ucontext),',
'                (unsigned long)RSI_sig(ucontext), (unsigned long)RDI_sig(ucontext),',
'                (unsigned long)RBP_sig(ucontext) );',
'            write( 2, b, n );'])
assert old in s, 'pf probe'
s = s.replace(old, new, 1)

open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched fix14')
