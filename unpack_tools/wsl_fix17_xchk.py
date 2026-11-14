NL = chr(10)
BS = chr(92)

p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
lines = open(p, encoding='utf-8', errors='replace').read().split(NL)
out = []
skip_next = False
for i, l in enumerate(lines):
    if 'XCHK' in l:
        if skip_next:
            skip_next = False
            continue
        # if the string was split across lines, the next line is the tail
        if not l.rstrip().endswith(';'):
            skip_next = True
        out.append('            { const char *fmt="XCHK hex=12345678 dec=%lu hexval=%lx decval=%lu' + BS + 'n"; char q[128]; int qn=snprintf(q,sizeof(q),fmt,(unsigned long)305419896UL,(unsigned long)0x054202d6UL,(unsigned long)0x054202d6UL); write(2,q,qn); }')
    else:
        out.append(l)
open(p, 'w', encoding='utf-8', newline=NL).write(NL.join(out))
print('rewritten')
