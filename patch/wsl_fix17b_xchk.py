NL = chr(10)
BS = chr(92)

p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
lines = open(p, encoding='utf-8', errors='replace').read().split(NL)
out = []
skip = False
for l in lines:
    if skip:
        skip = False
        continue
    if 'XCHK' in l:
        if not l.rstrip().endswith(';'):
            skip = True
        out.append('            { const char *fmt="XCHK hex=12345678 dec=%lu hexval=%lx decval=%lu' + BS + 'n"; char q[128]; int qn=snprintf(q,sizeof(q),fmt,(unsigned long)305419896UL,(unsigned long)0x054202d6UL,(unsigned long)0x054202d6UL); write(2,q,qn); }')
    elif l.startswith('"; char q[128]'):
        continue
    else:
        out.append(l)
open(p, 'w', encoding='utf-8', newline=NL).write(NL.join(out))
print('rewritten')
