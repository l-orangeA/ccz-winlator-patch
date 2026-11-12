NL = chr(10)
BS = chr(92)
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
if 'HWBP_LEN' in s2:
    print('already')
else:
    old = '                        srec.ExceptionAddress = (void *)RIP_sig(ucontext);'
    new = NL.join([
'                        {   /* HWBP_LEN: report the trap at the next instruction */',
'                          unsigned char *ip = (unsigned char *)RIP_sig(ucontext);',
'                          int len = 1;',
'                          unsigned char op = ip[0];',
'                          if (op == 0x8b || op == 0x89 || op == 0x8a || op == 0x88 ||',
'                              op == 0x8d || op == 0x85 || op == 0x8f || op == 0x03 ||',
'                              op == 0x39 || op == 0x3b || op == 0x01)',
'                          {',
'                              int modrm = ip[1], mod = modrm >> 6, rm = modrm & 7;',
'                              len = 2;',
'                              if (rm == 4 && mod != 3) len += 1;',
'                              if (mod == 0 && rm == 5) len += 4;',
'                              else if (mod == 1) len += 1;',
'                              else if (mod == 2) len += 4;',
'                          }',
'                          else if (op >= 0x50 && op <= 0x5f) len = 1;',
'                          srec.ExceptionAddress = (void *)((ULONG_PTR)ip + len);',
'                        }'])
    assert old in s2, 'srec addr'
    s2 = s2.replace(old, new, 1)
    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('patched HWBP_LEN')
