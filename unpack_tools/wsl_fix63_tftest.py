import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()

old = '''        wow_frame->EFlags = context->EFlags;
        wow_frame->SegCs  = cs32_sel;'''
new = '''        wow_frame->EFlags = context->EFlags;
        if (getenv( "WINE_TF_TEST" )) { wow_frame->EFlags |= 0x100; { char b[96]; int n = snprintf(b,sizeof(b),"TF_SET eip=%lx esp=%lx\\n",(unsigned long)context->Eip,(unsigned long)context->Esp); write(2,b,n); } }  /* TF_TEST */
        wow_frame->SegCs  = cs32_sel;'''
assert old in s, 'eflags'
s = s.replace(old, new, 1)

old2 = '''    ucontext_t *ucontext = init_handler( sigcontext );

    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP && hwbp_step_active())   /* HWBP_STEP */'''
new2 = '''    ucontext_t *ucontext = init_handler( sigcontext );

    { char b[112]; int n = snprintf(b,sizeof(b),"TRAP sig=%d code=%d rip=%lx trap=%lx tf=%d\\n", signal, siginfo->si_code, (unsigned long)RIP_sig(ucontext), (unsigned long)TRAP_sig(ucontext), (EFL_sig(ucontext)>>8)&1); write(2,b,n); }  /* TF_TEST */

    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP && hwbp_step_active())   /* HWBP_STEP */'''
assert old2 in s, 'trap log'
s = s.replace(old2, new2, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('tf test added')
