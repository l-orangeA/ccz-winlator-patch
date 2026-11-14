import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
if 'HWBP_EMU_INSN' in s:
    print('already'); raise SystemExit

anchor = '''static void segv_handler( int signal, siginfo_t *siginfo, void *sigcontext )'''
assert anchor in s, 'segv def'
emu = r'''/* HWBP_EMU_INSN: emulate one guest instruction that touched a watched page */
static ULONG_PTR hwbp_getreg( ucontext_t *uc, int n )
{
    switch (n)
    {
    case 0: return RAX_sig(uc) & 0xffffffffu;
    case 1: return RCX_sig(uc) & 0xffffffffu;
    case 2: return RDX_sig(uc) & 0xffffffffu;
    case 3: return RBX_sig(uc) & 0xffffffffu;
    case 4: return RSP_sig(uc) & 0xffffffffu;
    case 5: return RBP_sig(uc) & 0xffffffffu;
    case 6: return RSI_sig(uc) & 0xffffffffu;
    default: return RDI_sig(uc) & 0xffffffffu;
    }
}

static void hwbp_setreg( ucontext_t *uc, int n, ULONG_PTR v )
{
    ULONG_PTR *p;
    switch (n)
    {
    case 0: p = &RAX_sig(uc); break;
    case 1: p = &RCX_sig(uc); break;
    case 2: p = &RDX_sig(uc); break;
    case 3: p = &RBX_sig(uc); break;
    case 4: p = &RSP_sig(uc); break;
    case 5: p = &RBP_sig(uc); break;
    case 6: p = &RSI_sig(uc); break;
    default: p = &RDI_sig(uc); break;
    }
    *p = (*p & ~0xffffffffull) | (v & 0xffffffffu);
}

static int hwbp_emu_modrm( ucontext_t *uc, const unsigned char *p, ULONG_PTR *ea, int *reg, int *is_mem )
{
    int mod = p[0] >> 6, rm = p[0] & 7, n = 1;
    ULONG_PTR base = 0;
    *reg = (p[0] >> 3) & 7;
    if (mod == 3) { *is_mem = 0; *ea = rm; return n; }
    *is_mem = 1;
    if (rm == 4)
    {
        int sib = p[n++];
        int ss = sib >> 6, idx = (sib >> 3) & 7, bs = sib & 7;
        ULONG_PTR idxv = (idx == 4) ? 0 : hwbp_getreg( uc, idx );
        if (bs == 5 && mod == 0) base = 0;
        else base = hwbp_getreg( uc, bs );
        base += idxv << ss;
    }
    else if (mod == 0 && rm == 5)
    {
        base = (ULONG_PTR)(unsigned int)(p[n] | (p[n+1] << 8) | (p[n+2] << 16) | ((unsigned)p[n+3] << 24));
        *ea = base;
        return n + 4;
    }
    else base = hwbp_getreg( uc, rm );
    if (mod == 1) { base += (ULONG_PTR)(int)(signed char)p[n]; n += 1; }
    else if (mod == 2) { base += (ULONG_PTR)(unsigned int)(p[n] | (p[n+1] << 8) | (p[n+2] << 16) | ((unsigned)p[n+3] << 24)); n += 4; }
    *ea = base & 0xffffffffu;
    return n;
}

static int hwbp_emu_insn( ucontext_t *uc, const unsigned char *ip )
{
    int i = 0, opsz = 4, n;
    unsigned char op;
    ULONG esp = (ULONG)RSP_sig(uc);
    while (1)
    {
        unsigned char c = ip[i];
        if (c == 0x66) { opsz = 2; i++; continue; }
        if (c == 0x67) return 0;
        if (c == 0xf0 || c == 0xf2 || c == 0xf3 || c == 0x2e || c == 0x36 || c == 0x3e ||
            c == 0x26 || c == 0x64 || c == 0x65) { i++; continue; }
        break;
    }
    op = ip[i];
    if (op >= 0x50 && op <= 0x57)          /* push reg */
    {
        int k = op - 0x50;
        if (opsz == 2) { esp -= 2; *(unsigned short *)(ULONG_PTR)esp = (unsigned short)hwbp_getreg(uc, k); }
        else           { esp -= 4; *(unsigned int *)(ULONG_PTR)esp = (unsigned int)hwbp_getreg(uc, k); }
        hwbp_setreg( uc, 4, esp ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op >= 0x58 && op <= 0x5f)          /* pop reg */
    {
        int k = op - 0x58;
        if (opsz == 2) { unsigned short v = *(unsigned short *)(ULONG_PTR)esp;
                         hwbp_setreg(uc, k, (hwbp_getreg(uc,k) & ~0xffffu) | v); esp += 2; }
        else           { unsigned int v = *(unsigned int *)(ULONG_PTR)esp;
                         hwbp_setreg(uc, k, v); esp += 4; }
        hwbp_setreg( uc, 4, esp ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op == 0x60 && opsz == 4)           /* pushad */
    {
        esp -= 32;
        ((unsigned int *)(ULONG_PTR)esp)[0] = (unsigned)RAX_sig(uc);
        ((unsigned int *)(ULONG_PTR)esp)[1] = (unsigned)RCX_sig(uc);
        ((unsigned int *)(ULONG_PTR)esp)[2] = (unsigned)RDX_sig(uc);
        ((unsigned int *)(ULONG_PTR)esp)[3] = (unsigned)RBX_sig(uc);
        ((unsigned int *)(ULONG_PTR)esp)[4] = (unsigned)(RSP_sig(uc) + 32);
        ((unsigned int *)(ULONG_PTR)esp)[5] = (unsigned)RBP_sig(uc);
        ((unsigned int *)(ULONG_PTR)esp)[6] = (unsigned)RSI_sig(uc);
        ((unsigned int *)(ULONG_PTR)esp)[7] = (unsigned)RDI_sig(uc);
        hwbp_setreg( uc, 4, esp ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op == 0x61 && opsz == 4)           /* popad */
    {
        unsigned *m = (unsigned *)(ULONG_PTR)esp;
        hwbp_setreg( uc, 7, m[0] ); hwbp_setreg( uc, 6, m[1] ); hwbp_setreg( uc, 5, m[2] );
        hwbp_setreg( uc, 3, m[4] ); hwbp_setreg( uc, 2, m[5] ); hwbp_setreg( uc, 1, m[6] ); hwbp_setreg( uc, 0, m[7] );
        hwbp_setreg( uc, 4, esp + 32 ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op == 0x9c)                        /* pushfd */
    {
        if (opsz == 2) { esp -= 2; *(unsigned short *)(ULONG_PTR)esp = (unsigned short)(EFL_sig(uc) & 0xffffu); }
        else { esp -= 4; *(unsigned int *)(ULONG_PTR)esp = (unsigned int)EFL_sig(uc); }
        hwbp_setreg( uc, 4, esp ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op == 0x9d)                        /* popfd */
    {
        if (opsz == 2) { unsigned short v = *(unsigned short *)(ULONG_PTR)esp;
                         EFL_sig(uc) = (EFL_sig(uc) & ~0xffffull) | v; esp += 2; }
        else { unsigned int v = *(unsigned int *)(ULONG_PTR)esp;
               EFL_sig(uc) = (EFL_sig(uc) & ~0xffffffffull) | v; esp += 4; }
        hwbp_setreg( uc, 4, esp ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op == 0x6a)                        /* push imm8 */
    {
        if (opsz == 2) { esp -= 2; *(unsigned short *)(ULONG_PTR)esp = (unsigned short)(signed char)ip[i+1]; }
        else { esp -= 4; *(unsigned int *)(ULONG_PTR)esp = (unsigned int)(int)(signed char)ip[i+1]; }
        hwbp_setreg( uc, 4, esp ); RIP_sig(uc) += i + 2; return 1;
    }
    if (op == 0x68)                        /* push imm32 */
    {
        unsigned int v = ip[i+1] | (ip[i+2] << 8) | (ip[i+3] << 16) | ((unsigned)ip[i+4] << 24);
        if (opsz == 2) { esp -= 2; *(unsigned short *)(ULONG_PTR)esp = (unsigned short)v; }
        else { esp -= 4; *(unsigned int *)(ULONG_PTR)esp = v; }
        hwbp_setreg( uc, 4, esp ); RIP_sig(uc) += i + 5; return 1;
    }
    if (op == 0x8b || op == 0x8a || op == 0x89 || op == 0x88 || op == 0x8d)
    {
        ULONG_PTR ea = 0; int reg, is_mem;
        n = hwbp_emu_modrm( uc, ip + i + 1, &ea, &reg, &is_mem );
        if (!is_mem) return 0;
        if (op == 0x8b) { hwbp_setreg( uc, reg, *(unsigned int *)(ULONG_PTR)ea ); }
        else if (op == 0x8a) { hwbp_setreg( uc, reg, (hwbp_getreg(uc,reg) & ~0xffu) | *(unsigned char *)(ULONG_PTR)ea ); }
        else if (op == 0x89) { *(unsigned int *)(ULONG_PTR)ea = (unsigned int)hwbp_getreg( uc, reg ); }
        else if (op == 0x88) { *(unsigned char *)(ULONG_PTR)ea = (unsigned char)hwbp_getreg( uc, reg ); }
        else { hwbp_setreg( uc, reg, ea ); }
        RIP_sig(uc) += i + 1 + n; return 1;
    }
    if (op == 0xff)                        /* group5: push r/m */
    {
        ULONG_PTR ea = 0; int reg, is_mem;
        n = hwbp_emu_modrm( uc, ip + i + 1, &ea, &reg, &is_mem );
        if (reg != 6) return 0;
        { unsigned int v = is_mem ? *(unsigned int *)(ULONG_PTR)ea : (unsigned int)hwbp_getreg( uc, ea & 7 );
          esp -= 4; *(unsigned int *)(ULONG_PTR)esp = v; }
        hwbp_setreg( uc, 4, esp ); RIP_sig(uc) += i + 1 + n; return 1;
    }
    return 0;
}

'''
s = s.replace(anchor, emu + anchor, 1)

old = '''                if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000)
                {   /* HWBP_TRAMP_SIG: guest access, let exactly this insn through */
                    unsigned char *ip = (unsigned char *)RIP_sig(ucontext);
                    int len = hwbp_insn_len( ip );
                    if (len > 0 && len < 16)
                    {
                        hwbp_tempbp_set( (ULONG_PTR)ip + len );
                        { char b[96]; int n = snprintf(b,sizeof(b),"TRAMP rip=%lx len=%d tgt=%lx\\n",(unsigned long)ip,len,(unsigned long)((ULONG_PTR)ip+len)); write(2,b,n); }
                    }
                    else
                    { char b[96]; int n = snprintf(b,sizeof(b),"TRAMP unknown rip=%lx op=%02x\\n",(unsigned long)ip, ip[0]); write(2,b,n); }
                }
                /* page already unprotected by hwbp_handle_rw_fault */
                leave_handler( ucontext );
                return;'''
new = '''                if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000)
                {   /* HWBP_EMU_INSN: emulate this guest instruction, keep the page armed */
                    unsigned char *ip = (unsigned char *)RIP_sig(ucontext);
                    int ok = hwbp_emu_insn( ucontext, ip );
                    hwbp_reprotect_all();
                    { char b[128]; int n = snprintf(b,sizeof(b),"EMU rip=%lx ok=%d newrip=%lx esp=%lx\\n",(unsigned long)ip, ok, (unsigned long)RIP_sig(ucontext), (unsigned long)(RSP_sig(ucontext)&0xffffffff)); write(2,b,n); }
                    if (!ok) { char b[96]; int n = snprintf(b,sizeof(b),"EMU unknown op=%02x rip=%lx\\n", ip[0], (unsigned long)ip); write(2,b,n); }
                }
                leave_handler( ucontext );
                return;'''
assert old in s, 'emupath'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('emulator added')
