NL = chr(10)
BS = chr(92)

p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'STKPAGE ' in s:
    print('already'); raise SystemExit
old = NL.join([
'                for (i = -0x20; i <= 0x10; i += 0x10)',
'                {',
'                    n = snprintf( b, sizeof(b),',
'                        "STKDUMPMEM %lx: %08lx %08lx %08lx %08lx' + BS + 'n",',
'                        (unsigned long)(a + i),',
'                        (unsigned long)*(DWORD *)(a + i),',
'                        (unsigned long)*(DWORD *)(a + i + 4),',
'                        (unsigned long)*(DWORD *)(a + i + 8),',
'                        (unsigned long)*(DWORD *)(a + i + 12) );',
'                    write( 2, b, n );',
'                }'])
new = NL.join([
'                for (i = -0x20; i <= 0x60; i += 0x10)',
'                {',
'                    n = snprintf( b, sizeof(b),',
'                        "STKDUMPMEM %lx: %08lx %08lx %08lx %08lx' + BS + 'n",',
'                        (unsigned long)(a + i),',
'                        (unsigned long)*(DWORD *)(a + i),',
'                        (unsigned long)*(DWORD *)(a + i + 4),',
'                        (unsigned long)*(DWORD *)(a + i + 8),',
'                        (unsigned long)*(DWORD *)(a + i + 12) );',
'                    write( 2, b, n );',
'                }',
'                {',
'                    ULONG_PTR pg = (ULONG_PTR)a & ~(ULONG_PTR)0xfff;',
'                    int k;',
'                    for (k = 0; k < 0x1000; k += 0x40)',
'                    {',
'                        n = snprintf( b, sizeof(b),',
'                            "STKPAGE %lx: %08lx %08lx %08lx %08lx %08lx %08lx %08lx %08lx' + BS + 'n",',
'                            (unsigned long)(pg + k),',
'                            (unsigned long)*(DWORD *)(pg + k), (unsigned long)*(DWORD *)(pg + k + 4),',
'                            (unsigned long)*(DWORD *)(pg + k + 8), (unsigned long)*(DWORD *)(pg + k + 12),',
'                            (unsigned long)*(DWORD *)(pg + k + 16), (unsigned long)*(DWORD *)(pg + k + 20),',
'                            (unsigned long)*(DWORD *)(pg + k + 24), (unsigned long)*(DWORD *)(pg + k + 28) );',
'                        write( 2, b, n );',
'                    }',
'                }',
'                {',
'                    WOW_TEB *wt = get_wow_teb( teb );',
'                    if (wt)',
'                    {',
'                        n = snprintf( b, sizeof(b),',
'                            "STKWOW dealloc=%lx limit=%lx base=%lx guar=%lx teb=%lx' + BS + 'n",',
'                            (unsigned long)wt->DeallocationStack, (unsigned long)wt->Tib.StackLimit,',
'                            (unsigned long)wt->Tib.StackBase, (unsigned long)wt->GuaranteedStackBytes,',
'                            (unsigned long)wt );',
'                        write( 2, b, n );',
'                    }',
'                }'])
assert old in s, 'dump loop'
s = s.replace(old, new, 1)
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched STKPAGE')
