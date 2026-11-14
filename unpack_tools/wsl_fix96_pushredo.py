import shutil

BASE = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c.base88'
P = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
shutil.copyfile(BASE, P)
s = open(P, encoding='utf-8').read()

key = 'int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );'
i = s.index(key)
ls = s.rfind('\n', 0, i) + 1
endkey = 'if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000 || (ULONG_PTR)siginfo->si_addr < 0x10000000)'
j = s.index(endkey, i)
le = s.rfind('\n', 0, j) + 1
new_fault = (
'                    int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );\n'
'                    /* Two ways out of the armed window:\n'
'                     *  - push-like store: box64 already moved ESP down to the slot\n'
'                     *    before the store trapped.  Roll ESP back by the operand size\n'
'                     *    and let box64 re-execute the whole instruction itself; a\n'
'                     *    plain replay would subtract again.\n'
'                     *  - anything else: nothing has happened yet, so let the guest\n'
'                     *    execute the instruction for real under TF and trap right\n'
'                     *    after it (box64 honours guest TF). */\n'
'                    {\n'
'                        int pd = hwbp_push_delta( (const unsigned char *)RIP_sig(ucontext) );\n'
'                        if (CS_sig(ucontext) == 0x23 && pd &&\n'
'                            (ULONG32)RSP_sig(ucontext) == (ULONG32)siginfo->si_addr)\n'
'                        {\n'
'                            RSP_sig(ucontext) = (RSP_sig(ucontext) & ~(ULONG_PTR)0xffffffffu)\n'
'                                                | (ULONG32)((ULONG32)siginfo->si_addr + pd);\n'
'                            hwbp_step_begin( rwpage );\n'
'                            EFL_sig(ucontext) |= 0x100;\n'
'                            { static int sc; if (sc++ < 4000) { unsigned char *q=(unsigned char *)RIP_sig(ucontext); char b[176]; int n = snprintf(b,sizeof(b),"PUSH_REDO idx=%d fa=%lx d=%d rip=%lx op=%02x%02x%02x\\n", idx, (unsigned long)siginfo->si_addr, pd, (unsigned long)RIP_sig(ucontext), q[0],q[1],q[2]); write(2,b,n); } }\n'
'                            leave_handler( ucontext );\n'
'                            return;\n'
'                        }\n'
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

HELPER = r'''
/* HWBP_PUSH: operand size of a push-like instruction (0 = not push-like).
 * When a push traps, box64 has already moved ESP to the slot; rolling ESP back
 * by this amount lets box64 re-execute the whole instruction by itself. */
static int hwbp_push_delta( const unsigned char *ip )
{
    unsigned char op = ip[0];
    int sz = 4;
    if (op == 0x66) { sz = 2; op = ip[1]; }
    if (op >= 0x50 && op <= 0x57) return sz;
    if (op == 0x68 || op == 0x6a || op == 0x9c) return sz;
    if (op == 0x60) return 4;                  /* pushad */
    if (op == 0xe8) return 4;                  /* call rel32 */
    if (op == 0xff && (((ip[1] >> 3) & 7) == 2 || ((ip[1] >> 3) & 7) == 6)) return 4;
    return 0;
}

'''
anchor = 'static void segv_handler( int signal, siginfo_t *siginfo, void *sigcontext )\n'
k = s.index(anchor)
s = s[:k] + HELPER + s[k:]

open(P, 'w', encoding='utf-8').write(s)
print('fix96 applied')
