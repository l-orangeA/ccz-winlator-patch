NL = chr(10)
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
old = '                          if (mod != 3 && (op == 0x8b || op == 0x8a || op == 0x89 || op == 0x88))'
new = '                          if ((ULONG_PTR)ip >= 0x400000 && (ULONG_PTR)ip < 0x650000 && mod != 3 && (op == 0x8b || op == 0x8a || op == 0x89 || op == 0x88))  /* HWBP_EMU_GUARD */'
if new in s2:
    print('already')
else:
    assert old in s2, 'emu if'
    s2 = s2.replace(old, new, 1)
    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('guard added')
