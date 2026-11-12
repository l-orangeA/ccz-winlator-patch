import io, sys, re

P = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(P, encoding='utf-8').read()
orig = s

# ---------------- 1) page-fault branch: TF-step instead of emulate ----------------
key = 'int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );'
i = s.index(key)
# back up to the start of the line
ls = s.rfind('\n', 0, i) + 1
# find the end of the rwpage block: the PF_ENTRY log right after it
endkey = 'if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000 || (ULONG_PTR)siginfo->si_addr < 0x10000000)'
j = s.index(endkey, i)
le = s.rfind('\n', 0, j) + 1       # start of that line
# the replacement must leave: close inner '{', close 'if (rwpage)', close outer '{'
new_fault = (
'                    int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );\n'
'                    /* TF_STEP: page is open now; let the guest execute this instruction\n'
'                     * for real, then trap right after it (box64 honours guest TF). */\n'
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

# ---------------- 2) TRAP_TRACE: finish the step ----------------
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
            amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << pend);
            hwbp_consume();
            rec.ExceptionAddress = (void *)RIP_sig(ucontext);
            save_context( &context, ucontext );
            setup_raise_exception( ucontext, &rec, &context );
            return;
        }
        leave_handler( ucontext );
        return;
    }
"""
assert old_step in s, 'step block not found'
s = s.replace(old_step, new_step, 1)

assert s != orig
open(P, 'w', encoding='utf-8').write(s)
print('patched ok')
