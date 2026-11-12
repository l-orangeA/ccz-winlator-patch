import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
bad = '                               { char b[128]; int n2 = snprintf(b,sizeof(b),"EMU store rip=%lx ea=%lx val=%lx eax=%lx\n",(unsigned long)RIP_sig(uc),(unsigned long)ea,(unsigned long)hwbp_getreg(uc,0)); write(2,b,n2); } }'
good = '                               { char b[128]; int n2 = snprintf(b,sizeof(b),"EMU store rip=%lx ea=%lx val=%lx eax=%lx\\n",(unsigned long)RIP_sig(uc),(unsigned long)ea,(unsigned long)hwbp_getreg(uc,reg),(unsigned long)hwbp_getreg(uc,0)); write(2,b,n2); } }'
if bad in s:
    s = s.replace(bad, good, 1)
    io.open(p, 'w', encoding='utf-8', newline='').write(s)
    print('fixed store log')
else:
    print('pattern not found')
