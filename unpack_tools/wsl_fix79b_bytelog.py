import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
old = '''                    { char b[128]; int n = snprintf(b,sizeof(b),"EMU rip=%lx ok=%d newrip=%lx esp=%lx\\n",(unsigned long)ip, ok, (unsigned long)RIP_sig(ucontext), (unsigned long)(RSP_sig(ucontext)&0xffffffff)); write(2,b,n); }'''
new = '''                    { char b[160]; int n = snprintf(b,sizeof(b),"EMU rip=%lx ok=%d newrip=%lx esp=%lx bytes=%02x%02x%02x%02x%02x%02x\\n",(unsigned long)ip, ok, (unsigned long)RIP_sig(ucontext), (unsigned long)(RSP_sig(ucontext)&0xffffffff), ip[0],ip[1],ip[2],ip[3],ip[4],ip[5]); write(2,b,n); }'''
assert old in s, 'emu log'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('emu byte log added')
