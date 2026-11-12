import shutil

BASE = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c.base88'
P = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
shutil.copyfile(BASE, P)
s = open(P, encoding='utf-8').read()

# ---------------- page fault branch ----------------
key = 'int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );'
i = s.index(key)
ls = s.rfind('\n', 0, i) + 1
endkey = 'if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000 || (ULONG_PTR)siginfo->si_addr < 0x10000000)'
j = s.index(endkey, i)
le = s.rfind('\n', 0, j) + 1
new_fault = (
'                    int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );\n'
'                    /* Two ways out of the armed window:\n'
'                     *  - store-type fault: box64 already decremented ESP before the\n'
'                     *    store trapped, so replaying the instruction under TF would\n'
'                     *    decrement it a second time.  Just perform the store.\n'
'                     *  - load-type fault: nothing has happened yet, so let the guest\n'
'                     *    execute the instruction for real under TF and trap right\n'
'                     *    after it (box64 honours guest TF). */\n'
'                    if (CS_sig(ucontext) == 0x23 && (ERROR_sig(ucontext) & 2) &&\n'
'                        hwbp_emu_push( ucontext, (unsigned long)siginfo->si_addr ))\n'
'                    {\n'
'                        hwbp_reprotect_all();\n'
'                        { static int sc; if (sc++ < 4000) { unsigned char *q=(unsigned char *)RIP_sig(ucontext); char b[160]; int n = snprintf(b,sizeof(b),"EMU_PUSH idx=%d fa=%lx next=%lx esp=%lx op=%02x\\n", idx, (unsigned long)siginfo->si_addr, (unsigned long)RIP_sig(ucontext), (unsigned long)(RSP_sig(ucontext)&0xffffffff), q[0]); write(2,b,n); } }\n'
'                        leave_handler( ucontext );\n'
'                        return;\n'
'                    }\n'
'                    if (CS_sig(ucontext) == 0x23)\n'
'                    {\n'
'                        hwbp_step_begin( rwpage );\n'
'                        EFL_sig(ucontext) |= 0x100;\n'
'                        { static int sc; if (sc++ < 4000) { unsigned char *q=(unsigned char *)RIP_sig(ucontext); char b[176]; int n = snprintf(b,sizeof(b),"STEP_BEGIN idx=%d rip=%lx fa=%lx esp=%lx op=%02x%02x%02x%02x\\n", idx, (unsigned long)RIP_sig(ucontext), (unsigned long)siginfo->si_addr, (unsigned long)(RSP_sig(ucontext)&0xffffffff), q[0],q[1],q[2],q[3]); write(2,b,n); } }\n'
'                    }\n'
'                    else\n'
'                    {\n'
'                        char b[96]; int n = snprintf(b,sizeof(b),"STEP_HOST fa=%lx cs=%lx\\n",(unsigned long)siginfo->si_addr,(unsigned long)CS_sig(ucontext)); write(2,b,n);\n'
'                    }\n'
'                    leave_handler( ucontext );\n'
'                    return;\n'
'                }\n'
'            }\n'
'        }\n'
)
s = s[:ls] + new_fault + s[le:]

old_step = """    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP && hwbp_step_active())   /* HWBP_STEP */
    {
        if ((ULONG_PTR)RIP_sig(ucontext) >= 0x10000000)
        {   /* still inside 64-bit bridge code: keep TF and continue */
            leave_handler( ucontext );
            return;
        }
        EFL_sig(ucontext) &= ~0x100;
        hwbp_step_end();
        leave_handler( ucontext );
        return;
    }
"""
new_step = """    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP && hwbp_step_active())   /* TF_STEP */
    {
        int pend = hwbp_take_pending();
        hwbp_step_end();
        EFL_sig(ucontext) &= ~0x100;
        { static int sc; if (sc++ < 4000) { char b[144]; int n = snprintf(b,sizeof(b),"STEP_END pend=%d rip=%lx cs=%lx efl=%lx\\n", pend, (unsigned long)RIP_sig(ucontext), (unsigned long)CS_sig(ucontext), (unsigned long)EFL_sig(ucontext)); write(2,b,n); } }
        if (pend >= 1 && pend <= 4 && CS_sig(ucontext) == 0x23)
        {
            amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << (pend - 1));
            hwbp_consume();
            rec.ExceptionCode = EXCEPTION_SINGLE_STEP;
            rec.ExceptionAddress = (void *)RIP_sig(ucontext);
            save_context( &context, ucontext );
            setup_raise_exception( ucontext, &rec, &context );
            return;
        }
        leave_handler( ucontext );
        return;
    }
"""
assert old_step in s
s = s.replace(old_step, new_step, 1)

# ---------------- push emulation ----------------
HELPER = r'''
/* HWBP_PUSH: box64 adjusts ESP before the store is performed, so when a push
 * traps the new stack top is already in RSP and the fault address is the slot.
 * Replaying the instruction under TF would subtract 4 again, so finish the
 * store ourselves and step over the instruction. */
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
    case 0: RAX_sig(ucontext) = (ULONG_PTR)v; break;
    case 1: RCX_sig(ucontext) = (ULONG_PTR)v; break;
    case 2: RDX_sig(ucontext) = (ULONG_PTR)v; break;
    case 3: RBX_sig(ucontext) = (ULONG_PTR)v; break;
    case 4: RSP_sig(ucontext) = (ULONG_PTR)v; break;
    case 5: RBP_sig(ucontext) = (ULONG_PTR)v; break;
    case 6: RSI_sig(ucontext) = (ULONG_PTR)v; break;
    default: RDI_sig(ucontext) = (ULONG_PTR)v; break;
    }
}

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
        if (base == 5 && mod == 0) { v += *(const ULONG32 *)(ip + len); len += 4; }
        else v += hwbp_get32( ucontext, base );
    }
    else if (rm == 5 && mod == 0) { v = *(const ULONG32 *)(ip + 1); len += 4; }
    else v = hwbp_get32( ucontext, rm );
    if (mod == 1) { v += (ULONG32)(signed char)ip[len]; len += 1; }
    else if (mod == 2) { v += *(const ULONG32 *)(ip + len); len += 4; }
    *ea = v;
    return len;
}

static int hwbp_emu_push( ucontext_t *ucontext, unsigned long fa )
{
    const unsigned char *ip = (const unsigned char *)RIP_sig(ucontext);
    unsigned char op = ip[0];
    int i;

    /* box64's fault error-code bit is not reliable, so use the stack top:
     * only when the fault address IS the current ESP did box64 already
     * decrement before the store (i.e. this really is a push). */
    if ((unsigned long)RSP_sig(ucontext) != fa) return 0;

    if (op >= 0x50 && op <= 0x57)                   /* push r32 */
    {
        *(ULONG32 *)fa = hwbp_get32( ucontext, op - 0x50 );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 1);
        return 1;
    }
    if (op == 0x68)                                 /* push imm32 */
    {
        *(ULONG32 *)fa = *(const ULONG32 *)(ip + 1);
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 5);
        return 1;
    }
    if (op == 0x6a)                                 /* push imm8 */
    {
        *(ULONG32 *)fa = (ULONG32)(signed char)ip[1];
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 2);
        return 1;
    }
    if (op == 0x9c)                                 /* pushfd */
    {
        *(ULONG32 *)fa = (ULONG32)EFL_sig(ucontext);
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 1);
        return 1;
    }
    if (op == 0x60)                                 /* pushad: we are at the EAX slot */
    {
        ULONG32 v[8];
        for (i = 0; i < 8; i++) v[i] = hwbp_get32( ucontext, i );
        for (i = 0; i < 8; i++) *(ULONG32 *)(fa - 4 * i) = v[i];
        hwbp_set32( ucontext, 4, (ULONG32)fa - 28 );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 1);
        return 1;
    }
    if (op == 0xe8)                                 /* call rel32 */
    {
        ULONG32 rel = *(const ULONG32 *)(ip + 1);
        *(ULONG32 *)fa = (ULONG32)(ULONG_PTR)(ip + 5);
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 5 + (int)rel);
        return 1;
    }
    if (op == 0xff && ((ip[1] >> 3) & 7) == 2)      /* call r/m32 */
    {
        ULONG32 ea, val;
        int len, mod = ip[1] >> 6;
        if (mod == 3) { val = hwbp_get32( ucontext, ip[1] & 7 ); len = 2; }
        else
        {
            len = 1 + hwbp_ea32( ucontext, ip + 1, &ea );
            if (len <= 1 || !ea) return 0;
            val = *(ULONG32 *)ea;
        }
        *(ULONG32 *)fa = (ULONG32)(ULONG_PTR)(ip + len);
        RIP_sig(ucontext) = (ULONG_PTR)val;
        return 1;
    }
    if (op == 0xff && ((ip[1] >> 3) & 7) == 6)      /* push r/m32 */
    {
        ULONG32 ea, val;
        int len, mod = ip[1] >> 6;
        if (mod == 3) { val = hwbp_get32( ucontext, ip[1] & 7 ); len = 2; }
        else
        {
            len = 1 + hwbp_ea32( ucontext, ip + 1, &ea );
            if (len <= 1 || !ea) return 0;
            val = *(ULONG32 *)ea;
        }
        *(ULONG32 *)fa = val;
        RIP_sig(ucontext) = (ULONG_PTR)(ip + len);
        return 1;
    }
    return 0;
}

'''
anchor = 'static void segv_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n'
k = s.index(anchor)
s = s[:k] + HELPER + s[k:]

open(P, 'w', encoding='utf-8').write(s)
print('fix94 applied')
