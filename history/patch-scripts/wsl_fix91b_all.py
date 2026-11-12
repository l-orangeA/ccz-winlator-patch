import shutil, re, sys

BASE = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c.base88'
P = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
shutil.copyfile(BASE, P)
s = open(P, encoding='utf-8').read()

# ---------------- fix89: page fault -> TF step ----------------
key = 'int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );'
i = s.index(key)
ls = s.rfind('\n', 0, i) + 1
endkey = 'if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000 || (ULONG_PTR)siginfo->si_addr < 0x10000000)'
j = s.index(endkey, i)
le = s.rfind('\n', 0, j) + 1
new_fault = (
'                    int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );\n'
'                    /* TF_STEP: page is open now; let the guest execute this instruction\n'
'                     * for real, then trap right after it (box64 honours guest TF).\n'
'                     * A CALL into the syscall dispatcher loses TF, so calls/rets\n'
'                     * are emulated exactly instead. */\n'
'                    if (CS_sig(ucontext) == 0x23 && hwbp_emu_stack( ucontext, idx, (unsigned long)siginfo->si_addr ))\n'
'                    {\n'
'                        hwbp_reprotect_all();\n'
'                        { static int sc; if (sc++ < 4000) { unsigned char *q=(unsigned char *)RIP_sig(ucontext); char b[160]; int n = snprintf(b,sizeof(b),"EMU_STACK idx=%d fa=%lx next=%lx esp=%lx op=%02x\\n", idx, (unsigned long)siginfo->si_addr, (unsigned long)RIP_sig(ucontext), (unsigned long)(RSP_sig(ucontext)&0xffffffff), q[0]); write(2,b,n); } }\n'
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

# ---------------- fix91: exact emulation of call/ret only ----------------
HELPER = r'''
/* HWBP_STACK: TF-stepping a CALL that enters the 64-bit syscall dispatcher
 * loses the trap flag, so the watch would silently die.  Emulate exactly the
 * few stack-adjusting control transfers that can show up in the armed window;
 * everything else is still executed for real under TF. */
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

static int hwbp_emu_stack( ucontext_t *ucontext, int idx, unsigned long fa )
{
    const unsigned char *ip = (const unsigned char *)RIP_sig(ucontext);
    ULONG32 esp = (ULONG32)RSP_sig(ucontext);
    unsigned char op = ip[0];

    (void)esp;
    if (idx != 0) return 0;                 /* only for non-matching accesses */
    /* box64 reports RSP already adjusted to the faulting store/load slot, so
     * fa (not RSP-4) is the authoritative stack top here. */
    if (op == 0xe8)                         /* call rel32 */
    {
        ULONG32 rel = *(const ULONG32 *)(ip + 1);
        *(ULONG32 *)fa = (ULONG32)(ULONG_PTR)(ip + 5);
        hwbp_set32( ucontext, 4, (ULONG32)fa );
        RIP_sig(ucontext) = (ULONG_PTR)(ip + 5 + (int)rel);
        return 1;
    }
    if (op == 0xc3)                         /* ret */
    {
        ULONG32 t = *(ULONG32 *)fa;
        hwbp_set32( ucontext, 4, (ULONG32)fa + 4 );
        RIP_sig(ucontext) = (ULONG_PTR)t;
        return 1;
    }
    if (op == 0xc2)                         /* ret imm16 */
    {
        ULONG32 t = *(ULONG32 *)fa;
        hwbp_set32( ucontext, 4, (ULONG32)fa + 4 + *(const unsigned short *)(ip + 1) );
        RIP_sig(ucontext) = (ULONG_PTR)t;
        return 1;
    }
    return 0;
}

'''
anchor = 'static void segv_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n'
k = s.index(anchor)
s = s[:k] + HELPER + s[k:]

open(P, 'w', encoding='utf-8').write(s)
print('fix89+90+91 applied')
