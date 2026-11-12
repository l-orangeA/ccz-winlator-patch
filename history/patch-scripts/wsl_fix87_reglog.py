import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
if 'HWBP_REGLOG' in s:
    print('already'); raise SystemExit

anchor = '''        RIP_sig(uc) += i + 1 + n;
        return 1;
    }
    return 0;
}

static void segv_handler'''
assert anchor in s, 'tail'
new = '''        RIP_sig(uc) += i + 1 + n;
        return 1;
    }
    return 0;
}

/* HWBP_REGLOG: dump registers around the first emulated instructions */
static void hwbp_reglog( const unsigned char *ip, ucontext_t *uc, int ok )
{
    static int n;
    char b[192];
    int len;
    if (n++ >= 400) return;
    len = snprintf( b, sizeof(b),
        "REGLOG #%d rip=%lx bytes=%02x%02x%02x%02x%02x%02x ok=%d eax=%lx ebx=%lx ecx=%lx edx=%lx esi=%lx edi=%lx ebp=%lx esp=%lx ef=%lx\\n",
        n, (unsigned long)RIP_sig(uc), ip[0],ip[1],ip[2],ip[3],ip[4],ip[5], ok,
        (unsigned long)(RAX_sig(uc)&0xffffffffu), (unsigned long)(RBX_sig(uc)&0xffffffffu),
        (unsigned long)(RCX_sig(uc)&0xffffffffu), (unsigned long)(RDX_sig(uc)&0xffffffffu),
        (unsigned long)(RSI_sig(uc)&0xffffffffu), (unsigned long)(RDI_sig(uc)&0xffffffffu),
        (unsigned long)(RBP_sig(uc)&0xffffffffu), (unsigned long)(RSP_sig(uc)&0xffffffffu),
        (unsigned long)EFL_sig(uc) );
    write( 2, b, len );
}

static void segv_handler'''
s = s.replace(anchor, new, 1)

# call it right after emulation
old2 = '''                    int ok = hwbp_emu_insn( ucontext, ip, (ULONG_PTR)siginfo->si_addr );'''
new2 = '''                    int ok = hwbp_emu_insn( ucontext, ip, (ULONG_PTR)siginfo->si_addr );
                    hwbp_reglog( ip, ucontext, ok );'''
assert old2 in s, 'call'
s = s.replace(old2, new2, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('reglog added')
