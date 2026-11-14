p='/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s=open(p,encoding='utf-8',errors='replace').read()
needle="        mprotect( (void *)page, end - page, PROT_READ | PROT_WRITE );"
if 'WINE_MPROTECT_STACK' in s:
    print('already'); raise SystemExit
if needle not in s:
    print('needle missing'); raise SystemExit
s=s.replace(needle, '        { static const char m[] = "WINE_MPROTECT_STACK\n"; write( 2, m, sizeof(m)-1 ); }\n'+needle, 1)
open(p,'w',encoding='utf-8',newline='\n').write(s)
print('dbg added')
