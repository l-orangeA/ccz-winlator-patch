import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()

start = s.find('/* HWBP_EMU_INSN: emulate one guest instruction that touched a watched page */')
end = s.find('static void segv_handler( int signal, siginfo_t *siginfo, void *sigcontext )')
assert start > 0 and end > start, 'range'

new = r'''/* HWBP_EMU_INSN: emulate one guest instruction that touched a watched page.
   Stack pointer is derived from the fault address: for a push the store target
   IS the new ESP, for a pop the load address IS the old ESP.  This avoids
   double-adjusting ESP when box64 already applied the decrement/increment. */
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

/* length of the ModRM operand (mod/rm/sib/disp), for advancing RIP */
static int hwbp_modrm_len( const unsigned char *p )
{
    int mod = p[0] >> 6, rm = p[0] & 7, n = 1;
    if (mod == 3) return n;
    if (rm == 4)
    {
        int sib = p[n++];
        int bs = sib & 7;
        if (bs == 5 && mod == 0) n += 4;
    }
    else if (mod == 0 && rm == 5) { n += 4; return n; }
    if (mod == 1) n += 1;
    else if (mod == 2) n += 4;
    return n;
}

static int hwbp_emu_insn( ucontext_t *uc, const unsigned char *ip, ULONG_PTR fa )
{
    int i = 0, opsz = 4, n, sz;
    unsigned char op;
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
    sz = (opsz == 2) ? 2 : 4;

    if (op >= 0x50 && op <= 0x57)          /* push reg */
    {
        int k = op - 0x50;
        if (opsz == 2) *(unsigned short *)(ULONG_PTR)fa = (unsigned short)hwbp_getreg(uc, k);
        else           *(unsigned int *)(ULONG_PTR)fa = (unsigned int)hwbp_getreg(uc, k);
        hwbp_setreg( uc, 4, fa ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op >= 0x58 && op <= 0x5f)          /* pop reg */
    {
        int k = op - 0x58;
        if (opsz == 2) { unsigned short v = *(unsigned short *)(ULONG_PTR)fa;
                         hwbp_setreg(uc, k, (hwbp_getreg(uc,k) & ~0xffffu) | v); }
        else           { unsigned int v = *(unsigned int *)(ULONG_PTR)fa;
                         hwbp_setreg(uc, k, v); }
        hwbp_setreg( uc, 4, fa + sz ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op == 0x60 && opsz == 4)           /* pushad: [fa]=EDI .. [fa+28]=EAX */
    {
        unsigned *m = (unsigned *)(ULONG_PTR)fa;
        m[0] = (unsigned)RDI_sig(uc);
        m[1] = (unsigned)RSI_sig(uc);
        m[2] = (unsigned)RBP_sig(uc);
        m[3] = (unsigned)(fa + 32);
        m[4] = (unsigned)RBX_sig(uc);
        m[5] = (unsigned)RDX_sig(uc);
        m[6] = (unsigned)RCX_sig(uc);
        m[7] = (unsigned)RAX_sig(uc);
        hwbp_setreg( uc, 4, fa ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op == 0x61 && opsz == 4)           /* popad */
    {
        unsigned *m = (unsigned *)(ULONG_PTR)fa;
        hwbp_setreg( uc, 7, m[0] ); hwbp_setreg( uc, 6, m[1] ); hwbp_setreg( uc, 5, m[2] );
        hwbp_setreg( uc, 3, m[4] ); hwbp_setreg( uc, 2, m[5] ); hwbp_setreg( uc, 1, m[6] ); hwbp_setreg( uc, 0, m[7] );
        hwbp_setreg( uc, 4, fa + 32 ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op == 0x9c)                        /* pushfd / pushfw */
    {
        if (opsz == 2) *(unsigned short *)(ULONG_PTR)fa = (unsigned short)(EFL_sig(uc) & 0xffffu);
        else *(unsigned int *)(ULONG_PTR)fa = (unsigned int)EFL_sig(uc);
        hwbp_setreg( uc, 4, fa ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op == 0x9d)                        /* popfd / popfw */
    {
        if (opsz == 2) { unsigned short v = *(unsigned short *)(ULONG_PTR)fa;
                         EFL_sig(uc) = (EFL_sig(uc) & ~0xffffull) | v; }
        else { unsigned int v = *(unsigned int *)(ULONG_PTR)fa;
               EFL_sig(uc) = (EFL_sig(uc) & ~0xffffffffull) | v; }
        hwbp_setreg( uc, 4, fa + sz ); RIP_sig(uc) += i + 1; return 1;
    }
    if (op == 0x6a)                        /* push imm8 */
    {
        if (opsz == 2) *(unsigned short *)(ULONG_PTR)fa = (unsigned short)(signed char)ip[i+1];
        else *(unsigned int *)(ULONG_PTR)fa = (unsigned int)(int)(signed char)ip[i+1];
        hwbp_setreg( uc, 4, fa ); RIP_sig(uc) += i + 2; return 1;
    }
    if (op == 0x68)                        /* push imm32 */
    {
        unsigned int v = ip[i+1] | (ip[i+2] << 8) | (ip[i+3] << 16) | ((unsigned)ip[i+4] << 24);
        if (opsz == 2) *(unsigned short *)(ULONG_PTR)fa = (unsigned short)v;
        else *(unsigned int *)(ULONG_PTR)fa = v;
        hwbp_setreg( uc, 4, fa ); RIP_sig(uc) += i + 5; return 1;
    }
    if (op == 0xe8)                        /* call rel32 */
    {
        unsigned int rel = ip[i+1] | (ip[i+2] << 8) | (ip[i+3] << 16) | ((unsigned)ip[i+4] << 24);
        unsigned int ret = (unsigned int)(RIP_sig(uc) + i + 5);
        *(unsigned int *)(ULONG_PTR)fa = ret;
        hwbp_setreg( uc, 4, fa ); RIP_sig(uc) = (ULONG_PTR)(ret + rel); return 1;
    }
    if (op == 0xc3)                        /* ret */
    {
        unsigned int v = *(unsigned int *)(ULONG_PTR)fa;
        hwbp_setreg( uc, 4, fa + 4 ); RIP_sig(uc) = v; return 1;
    }
    if (op == 0xc2)                        /* ret imm16 */
    {
        unsigned int v = *(unsigned int *)(ULONG_PTR)fa;
        unsigned int imm = ip[i+1] | (ip[i+2] << 8);
        hwbp_setreg( uc, 4, fa + 4 + imm ); RIP_sig(uc) = v; return 1;
    }
    if (op == 0xff)                        /* group5: push/call r/m */
    {
        int reg = (ip[i+1] >> 3) & 7;
        n = hwbp_modrm_len( ip + i + 1 );
        if (reg == 6)   /* push r/m */
        {
            unsigned int v = (unsigned int)*(unsigned int *)(ULONG_PTR)fa;
            if (opsz == 2) { v = *(unsigned short *)(ULONG_PTR)fa; *(unsigned short *)(ULONG_PTR)fa = (unsigned short)v; }
            else *(unsigned int *)(ULONG_PTR)fa = v;
            hwbp_setreg( uc, 4, fa ); RIP_sig(uc) += i + 1 + n; return 1;
        }
        if (reg == 2)   /* call r/m: target came from memory at fa, return addr goes to fa-4 */
        {
            unsigned int tgt = *(unsigned int *)(ULONG_PTR)fa;
            unsigned int ret = (unsigned int)(RIP_sig(uc) + i + 1 + n);
            *(unsigned int *)(ULONG_PTR)(fa - 4) = ret;
            hwbp_setreg( uc, 4, fa - 4 ); RIP_sig(uc) = tgt; return 1;
        }
        return 0;
    }
    if (op == 0x8b || op == 0x8a || op == 0x89 || op == 0x88 || op == 0x8d ||
        op == 0xc7 || op == 0xc6 || op == 0x80 || op == 0x81 || op == 0x83)
    {
        int reg = (ip[i+1] >> 3) & 7;
        n = hwbp_modrm_len( ip + i + 1 );
        if (((ip[i+1] >> 6) & 3) == 3) return 0;      /* register form cannot fault */
        if (op == 0x8b) { unsigned int v = (opsz == 2) ? *(unsigned short *)(ULONG_PTR)fa : *(unsigned int *)(ULONG_PTR)fa;
                          if (opsz == 2) hwbp_setreg( uc, reg, (hwbp_getreg(uc,reg) & ~0xffffu) | v ); else hwbp_setreg( uc, reg, v ); }
        else if (op == 0x8a) { hwbp_setreg( uc, reg, (hwbp_getreg(uc,reg) & ~0xffu) | *(unsigned char *)(ULONG_PTR)fa ); }
        else if (op == 0x89) { if (opsz == 2) *(unsigned short *)(ULONG_PTR)fa = (unsigned short)hwbp_getreg( uc, reg );
                               else *(unsigned int *)(ULONG_PTR)fa = (unsigned int)hwbp_getreg( uc, reg ); }
        else if (op == 0x88) { *(unsigned char *)(ULONG_PTR)fa = (unsigned char)hwbp_getreg( uc, reg ); }
        else if (op == 0x8d) { return 0; }            /* lea cannot fault */
        else if (op == 0xc7) { unsigned int imm = ip[i+1+n] | (ip[i+2+n] << 8) | (ip[i+3+n] << 16) | ((unsigned)ip[i+4+n] << 24);
                               if (opsz == 2) *(unsigned short *)(ULONG_PTR)fa = (unsigned short)imm; else *(unsigned int *)(ULONG_PTR)fa = imm;
                               RIP_sig(uc) += i + 1 + n + (opsz == 2 ? 2 : 4); return 1; }
        else if (op == 0xc6) { *(unsigned char *)(ULONG_PTR)fa = ip[i+1+n]; RIP_sig(uc) += i + 1 + n + 1; return 1; }
        else
        {
            unsigned int imm, cur, res;
            int width = (op == 0x80) ? 1 : sz;
            if (op == 0x81) imm = ip[i+1+n] | (ip[i+2+n] << 8) | (ip[i+3+n] << 16) | ((unsigned)ip[i+4+n] << 24);
            else imm = (unsigned int)(int)(signed char)ip[i+1+n];
            cur = (width == 1) ? *(unsigned char *)(ULONG_PTR)fa : (width == 2) ? *(unsigned short *)(ULONG_PTR)fa : *(unsigned int *)(ULONG_PTR)fa;
            switch (reg)
            {
            case 0: res = cur + imm; break;
            case 1: res = cur | imm; break;
            case 4: res = cur & imm; break;
            case 5: res = cur - imm; break;
            case 6: res = cur ^ imm; break;
            default: return 0;
            }
            if (width == 1) *(unsigned char *)(ULONG_PTR)fa = (unsigned char)res;
            else if (width == 2) *(unsigned short *)(ULONG_PTR)fa = (unsigned short)res;
            else *(unsigned int *)(ULONG_PTR)fa = res;
            RIP_sig(uc) += i + 1 + n + ((op == 0x81) ? (opsz == 2 ? 2 : 4) : 1);
            return 1;
        }
        RIP_sig(uc) += i + 1 + n;
        return 1;
    }
    return 0;
}

'''
s = s[:start] + new + s[end:]

# update the call site to pass the fault address
s = s.replace('int ok = hwbp_emu_insn( ucontext, ip );',
              'int ok = hwbp_emu_insn( ucontext, ip, (ULONG_PTR)siginfo->si_addr );', 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('emulator rewritten (fault-address based)')
