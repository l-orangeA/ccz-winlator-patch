import io
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
if 'HWBP_TRAMP_SIG' in s:
    print('already'); raise SystemExit

# externs
old = '''extern void hwbp_step_begin( char *page );  /* HWBP_STEP */'''
new = '''extern void hwbp_hold_begin( void );  /* HWBP_TRAMP_SIG */
extern void hwbp_reprotect_all( void );
extern int hwbp_tempbp_set( unsigned long addr );
extern int hwbp_tempbp_check( unsigned long addr );
extern void hwbp_tempbp_clear_all( void );
extern int hwbp_insn_len( const unsigned char *p );
extern void hwbp_step_begin( char *page );  /* HWBP_STEP */'''
assert old in s, 'extern'
s = s.replace(old, new, 1)

# rw-page non-match path -> trampoline
old2 = '''                hwbp_step_begin( rwpage );   /* HWBP_STEP: let one insn through, keep page armed */
                EFL_sig(ucontext) |= 0x100;
                leave_handler( ucontext );
                return;'''
new2 = '''                if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000)
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
assert old2 in s, 'step path'
s = s.replace(old2, new2, 1)

# trap handler: our temp int3
old3 = '''    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP && hwbp_step_active())   /* HWBP_STEP */'''
new3 = '''    if (TRAP_sig(ucontext) == TRAP_x86_BPTFLT)   /* HWBP_TRAMP_SIG: our temp int3? */
    {
        ULONG_PTR a = (ULONG_PTR)RIP_sig(ucontext) - 1;
        if (hwbp_tempbp_check( a ))
        {
            hwbp_reprotect_all();
            RIP_sig(ucontext) = a;
            { char b[96]; int n = snprintf(b,sizeof(b),"TRAMP hit addr=%lx\\n",(unsigned long)a); write(2,b,n); }
            leave_handler( ucontext );
            return;
        }
    }

    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP && hwbp_step_active())   /* HWBP_STEP */'''
assert old3 in s, 'trap hook'
s = s.replace(old3, new3, 1)

# hold around set_thread_wow64_context: insert at entry after the WCTX probe block, and end before returns
old4 = '''    /* debug registers require a server call */
    if (self && (flags & CONTEXT_I386_DEBUG_REGISTERS))'''
new4 = '''    hwbp_hold_begin();   /* HWBP_TRAMP_SIG: host reads the 32-bit context below */

    /* debug registers require a server call */
    if (self && (flags & CONTEXT_I386_DEBUG_REGISTERS))'''
assert old4 in s, 'hold begin'
s = s.replace(old4, new4, 1)

open(p, 'w', encoding='utf-8', newline='').write(s)
print('signal trampoline added (hold begin only)')
