p = '/root/wine0905/dlls/ntdll/unix/server.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = '    if ((ULONG_PTR)context->Rip < 0x10000000)\n    {\n        char b[160]; int n = snprintf( b, sizeof(b),\n'
if 'static int ntc' in s:
    print('already')
else:
    assert old in s, 'probe head'
    new = ('    { static int ntc; if (ntc < 400)\n    {\n'
           '        char b[160]; int n; ntc++; n = snprintf( b, sizeof(b),\n')
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline='\n').write(s)
    print('widened')
