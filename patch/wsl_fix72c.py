import io, re
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
i = s.find('else if (op == 0x89)')
j = s.find('else if (op == 0x88)')
assert i > 0 and j > i, 'range'
old = s[i:j]
print('OLD BLOCK:', repr(old[:400]))
new = ('else if (op == 0x89) { *(unsigned int *)(ULONG_PTR)ea = (unsigned int)hwbp_getreg( uc, reg );\n'
       '                               { char b[128]; int n2 = snprintf(b,sizeof(b),"EMU store rip=%lx ea=%lx val=%lx eax=%lx\\n",(unsigned long)RIP_sig(uc),(unsigned long)ea,(unsigned long)hwbp_getreg(uc,reg),(unsigned long)hwbp_getreg(uc,0)); write(2,b,n2); } }\n        ')
s = s[:i] + new + s[j:]
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('replaced')
