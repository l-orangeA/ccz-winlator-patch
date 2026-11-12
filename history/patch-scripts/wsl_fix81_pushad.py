import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
old = '''    if (op == 0x60 && opsz == 4)           /* pushad: [fa]=EDI .. [fa+28]=EAX */
    {
        unsigned *m = (unsigned *)(ULONG_PTR)fa;
        m[0] = (unsigned)RDI_sig(uc);
        m[1] = (unsigned)RSI_sig(uc);
        m[2] = (unsigned)RBP_sig(uc);
        m[3] = (unsigned)(fa + 32);
        m[4] = (unsigned)RBX_sig(uc);
        m[5] = (unsigned)RDX_sig(uc);
        m[6] = (unsigned)RCX_sig(uc);
        m[7] = (unsigned)RAX_sig(uc);
        hwbp_setreg( uc, 4, fa ); RIP_sig(uc) += i + 1; return 1;
    }'''
new = '''    if (op == 0x60 && opsz == 4)           /* pushad: box64 pushes one by one, so the
                                              fault lies on the first sub-push (EAX) at ESP-4 */
    {
        ULONG_PTR base = fa - 28;          /* block base = ESP-32 */
        unsigned *m = (unsigned *)base;
        m[0] = (unsigned)RDI_sig(uc);
        m[1] = (unsigned)RSI_sig(uc);
        m[2] = (unsigned)RBP_sig(uc);
        m[3] = (unsigned)(base + 32);
        m[4] = (unsigned)RBX_sig(uc);
        m[5] = (unsigned)RDX_sig(uc);
        m[6] = (unsigned)RCX_sig(uc);
        m[7] = (unsigned)RAX_sig(uc);
        hwbp_setreg( uc, 4, base ); RIP_sig(uc) += i + 1; return 1;
    }'''
assert old in s, 'pushad'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('pushad base fixed')
