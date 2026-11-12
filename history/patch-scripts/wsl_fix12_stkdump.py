NL = chr(10)
BS = chr(92)

p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'STKDUMP stack=' in s:
    print('already'); raise SystemExit
old = NL.join([
'        if (!rec.ExceptionCode)',
'        {',
'            leave_handler( ucontext );',
'            return;',
'        }',
'        break;'])
new = NL.join([
'        {',
'            TEB *teb = NtCurrentTeb();',
'            char b[256]; int n = snprintf( b, sizeof(b),',
'                "STKDUMP stack=%lx addr=%lx start=%lx limit=%lx end=%lx guar=%lx' + BS + 'n",',
'                (unsigned long)RSP_sig(ucontext), (unsigned long)siginfo->si_addr,',
'                (unsigned long)teb->DeallocationStack, (unsigned long)teb->Tib.StackLimit,',
'                (unsigned long)teb->Tib.StackBase, (unsigned long)teb->GuaranteedStackBytes );',
'            write( 2, b, n );',
'            {',
'                ULONG_PTR a = (ULONG_PTR)siginfo->si_addr & ~(ULONG_PTR)0xf;',
'                int i;',
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
'                }',
'            }',
'        }',
'        if (!rec.ExceptionCode)',
'        {',
'            leave_handler( ucontext );',
'            return;',
'        }',
'        break;'])
assert old in s, 'stkflt block'
s = s.replace(old, new, 1)
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('patched STKDUMP')
