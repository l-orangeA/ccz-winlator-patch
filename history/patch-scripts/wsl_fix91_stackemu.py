import re

P = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(P, encoding='utf-8').read()
orig = s

HELPER = r'''
/* HWBP_STACK: exact emulation of the stack-touching instructions that show up
 * inside the armed window.  TF-stepping these is unsafe: a CALL into the
 * syscall dispatcher loses the trap flag, so the watch would silently die. */
static ULONG32 hwbp_get32( ucontext_t *ucontext, int i )
{
    switch (i)
    {
    case 0: return (ULONG32)RAX_sig(ucontext);
    case 1: return (ULONG32)RCX_sig(ucontext);
    case 2: return (ULONG32)RDX_sig(ucontext);
    case 3: return (ULONG32)RBX_sig(ucontext);
    case 4: return (ULONG32)RSP_sig(ucontext);
    case 5: return (ULONG32)RBP_sig(ucontext);
    case 6: return (ULONG32)RSI_sig(ucontext);
    default: return (ULONG32)RDI_sig(ucontext);
    }
}

static void hwbp_set32( ucontext_t *ucontext, int i, ULONG32 v )
{
    switch (i)
    {
    case 0: RAX_sig(ucontext) = (ULONG_PTR)(ULONG32)v; break;
    case 1: RCX_sig(ucontext) = (ULONG_PTR)(ULONG32)v; break;
    case 2: RDX_sig(ucontext) = (ULONG_PTR)(ULONG32)v; break;
    case 3: RBX_sig(ucontext) = (ULONG_PTR)(ULONG32)v; break;
    case 4: RSP_sig(ucontext) = (ULONG_PTR)(ULONG32)v; break;
    case 5: RBP_sig(ucontext) = (ULONG_PTR)(ULONG32)v; break;
    case 6: RSI_sig(ucontext) = (ULONG_PTR)(ULONG32)v; break;
    default: RDI_sig(ucontext) = (ULONG_PTR)(ULONG32)v; break;
    }
}

/* effective address of a mod!=3 r/m32; returns length consumed from ip (which
 * points at the modrm byte) or 0 if unsupported */
static int hwbp_ea32( ucontext_t *ucontext, const unsigned char *ip, ULONG32 *ea )
{
    int mod = ip[0] >> 6, rm = ip[0] & 7, len = 1;
    ULONG32 v;
    if (mod == 3) return 0;
    if (rm == 4)
    {
        int scale, index, base;
        unsigned char sib = ip[1];
        scale = sib >> 6; index = (sib >> 3) & 7; base = sib & 7; len = 2;
        v = (index == 4) ? 0 : hwbp_get32( ucontext, index ) << scale;
        if (base == 5 && mod == 0)
        {
            v += *(const ULONG32 *)(ip + len); len += 4;
        }
        else v += hwbp_get32( ucontext, base );
    }
    else if (rm == 5 && mod == 0)
    {
        v = *(const ULONG32 *)(ip + 1); len += 4;
    }
    else v = hwbp_get32( ucontext, rm );
    if (mod == 1) { v += (ULONG32)(signed char)ip[len]; len += 1; }
    else if (mod == 2) { v += *(const ULONG32 *)(ip + len); len += 4; }
    *ea = v;
    return len;
}

static int hwbp_emu_stack( ucontext_t *ucontext )
{
    const unsigned char *ip = (const unsigned char *)RIP_sig(ucontext);
    ULONG32 esp = (ULONG32)RSP_sig(ucontext);
    unsigned char op = ip[0];
    int i;

    if (op >= 0x50 && op <= 0x57)               /* push r32 */
    {
        ULONG32 v = hwbp_get32( ucontext, op - 0x50 );
        esp -= 4; *(ULONG32 *)esp = v;
        hwbp_set32( ucontext, 4, esp );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 1);
        return 1;
    }
    if (op >= 0x58 && op <= 0x5f)               /* pop r32 */
    {
        ULONG32 v = *(ULONG32 *)esp;
        esp += 4;
        hwbp_set32( ucontext, 4, esp );
        hwbp_set32( ucontext, op - 0x58, v );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 1);
        return 1;
    }
    if (op == 0x68)                             /* push imm32 */
    {
        ULONG32 v = *(const ULONG32 *)(ip + 1);
        esp -= 4; *(ULONG32 *)esp = v;
        hwbp_set32( ucontext, 4, esp );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 5);
        return 1;
    }
    if (op == 0x6a)                             /* push imm8 */
    {
        ULONG32 v = (ULONG32)(signed char)ip[1];
        esp -= 4; *(ULONG32 *)esp = v;
        hwbp_set32( ucontext, 4, esp );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 2);
        return 1;
    }
    if (op == 0x9c)                             /* pushfd */
    {
        ULONG32 v = (ULONG32)EFL_sig(ucontext);
        esp -= 4; *(ULONG32 *)esp = v;
        hwbp_set32( ucontext, 4, esp );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 1);
        return 1;
    }
    if (op == 0x9d)                             /* popfd */
    {
        ULONG32 v = *(ULONG32 *)esp;
        esp += 4;
        hwbp_set32( ucontext, 4, esp );
        EFL_sig(ucontext) = (EFL_sig(ucontext) & ~0x00040fd5u) | (v & 0x00040fd5u);
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 1);
        return 1;
    }
    if (op == 0x60)                             /* pushad */
    {
        ULONG32 v[8];
        for (i = 0; i < 8; i++) v[i] = hwbp_get32( ucontext, i );
        for (i = 7; i >= 0; i--) { esp -= 4; *(ULONG32 *)esp = v[i]; }
        hwbp_set32( ucontext, 4, esp );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 1);
        return 1;
    }
    if (op == 0x61)                             /* popad */
    {
        ULONG32 v[8];
        for (i = 0; i < 8; i++) { v[i] = *(ULONG32 *)esp; esp += 4; }
        for (i = 0; i < 8; i++) if (i != 4) hwbp_set32( ucontext, i, v[i] );
        hwbp_set32( ucontext, 4, esp );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 1);
        return 1;
    }
    if (op == 0xe8)                             /* call rel32 */
    {
        ULONG32 rel = *(const ULONG32 *)(ip + 1);
        esp -= 4; *(ULONG32 *)esp = (ULONG32)(ULONG_PTR)(ip + 5);
        hwbp_set32( ucontext, 4, esp );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 5 + (int)rel);
        return 1;
    }
    if (op == 0xc3)                             /* ret */
    {
        ULONG32 t = *(ULONG32 *)esp;
        esp += 4;
        hwbp_set32( ucontext, 4, esp );
        RIP_sig(ucontext) = (ULONG_PTR)t;
        return 1;
    }
    if (op == 0xc2)                             /* ret imm16 */
    {
        ULONG32 t = *(ULONG32 *)esp;
        esp += 4 + *(const unsigned short *)(ip + 1);
        hwbp_set32( ucontext, 4, esp );
        RIP_sig(ucontext) = (ULONG_PTR)t;
        return 1;
    }
    if (op == 0xff)                             /* call/push r/m32, inc/dec */
    {
        int reg = (ip[1] >> 3) & 7, mod = ip[1] >> 6;
        ULONG32 ea, val;
        int len;
        if (reg != 2 && reg != 6) return 0;
        if (mod == 3)
        {
            val = hwbp_get32( ucontext, ip[1] & 7 );
            len = 2;
        }
        else
        {
            len = 1 + hwbp_ea32( ucontext, ip + 1, &ea );
            if (!len || !ea) return 0;
            val = *(ULONG32 *)ea;
        }
        esp -= 4; *(ULONG32 *)esp = (reg == 2) ? (ULONG32)(ULONG_PTR)(ip + len) : val;
        hwbp_set32( ucontext, 4, esp );
        RIP_sig(ucontext) = (reg == 2) ? (ULONG_PTR)val : (ULONG_PTR)(ip + len);
        return 1;
    }
    if (op == 0x8f && (ip[1] >> 3) == 0)        /* pop r/m32 */
    {
        int mod = ip[1] >> 6;
        ULONG32 ea, val = *(ULONG32 *)esp;
        int len;
        esp += 4;
        hwbp_set32( ucontext, 4, esp );
        if (mod == 3) { hwbp_set32( ucontext, ip[1] & 7, val ); len = 2; }
        else
        {
            len = 1 + hwbp_ea32( ucontext, ip + 1, &ea );
            if (!len || !ea) return 0;
            *(ULONG32 *)ea = val;
        }
        RIP_sig(ucontext) = (ULONG_PTR)(ip + len);
        return 1;
    }
    return 0;
}

'''

anchor = 'static void trap_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n'
i = s.index(anchor)
s = s[:i] + HELPER + s[i:]

old = """                    if (CS_sig(ucontext) == 0x23)
                    {
                        hwbp_step_begin( rwpage );"""
new = """                    if (CS_sig(ucontext) == 0x23 && idx == 0 && hwbp_emu_stack( ucontext ))
                    {
                        hwbp_reprotect_all();
                        { static int sc; if (sc++ < 4000) { unsigned char *q=(unsigned char *)RIP_sig(ucontext); char b[160]; int n = snprintf(b,sizeof(b),"EMU_STACK fa=%lx next=%lx esp=%lx op=%02x\\n", (unsigned long)siginfo->si_addr, (unsigned long)RIP_sig(ucontext), (unsigned long)(RSP_sig(ucontext)&0xffffffff), q[0]); write(2,b,n); } }
                        leave_handler( ucontext );
                        return;
                    }
                    if (CS_sig(ucontext) == 0x23)
                    {
                        hwbp_step_begin( rwpage );"""
assert old in s
s = s.replace(old, new, 1)

assert s != orig
open(P, 'w', encoding='utf-8').write(s)
print('fix91 patched')
