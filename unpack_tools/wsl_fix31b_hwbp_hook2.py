import re
NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_HOOK2' in s:
    print('already')
else:
    pat = re.compile(r'^(\s*)amd64_thread_data\(\)->dr7 = context->Dr7;$', re.M)
    def repl(m):
        ind = m.group(1)
        if len(ind) < 8:
            return m.group(0)
        return (m.group(0) + NL +
                ind + 'hwbp_update( (unsigned long)context->Dr0, (unsigned long)context->Dr1, /* HWBP_HOOK2 */' + NL +
                ind + '             (unsigned long)context->Dr2, (unsigned long)context->Dr3,' + NL +
                ind + '             (unsigned long)context->Dr7 );')
    s2, n = pat.subn(repl, s)
    open(p, 'w', encoding='utf-8', newline=NL).write(s2)
    print('matched', n)
