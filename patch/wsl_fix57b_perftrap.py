NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()

if 'HWBP_PERF_TRAP' in s:
    print('already')
    raise SystemExit

# extern decls near the other hwbp extern
old = 'extern int hwbp_handle_rw_fault( char *page, const void *addr );  /* HWBP_V2_SIG */'
assert old in s, 'extern'
s = s.replace(old, old + NL + 'extern int hwbp_perf_owner( int fd );  /* HWBP_PERF_TRAP */' + NL + 'extern int hwbp_perf_mark( int idx );', 1)

old2 = NL.join([
'    ucontext_t *ucontext = init_handler( sigcontext );',
'',
'    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP)  /* HWBP_IDX_SIG */'])
new2 = NL.join([
'    ucontext_t *ucontext = init_handler( sigcontext );',
'',
'    if (siginfo->si_code == SI_SIGIO)   /* HWBP_PERF_TRAP: hardware data breakpoint */',
'    {',
'        int idx = hwbp_perf_owner( siginfo->si_fd );',
'        if (idx)',
'        {',
'            if (hwbp_perf_mark( idx ) && CS_sig(ucontext) == 0x23)',
'            {',
'                amd64_thread_data()->dr6 = 0xffff0ff0u | (1u << idx);',
'                rec.ExceptionAddress = (void *)RIP_sig(ucontext);',
'                save_context( &context, ucontext );',
'                setup_raise_exception( ucontext, &rec, &context );',
'                return;',
'            }',
'            leave_handler( ucontext );',
'            return;',
'        }',
'    }',
'',
'    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP)  /* HWBP_IDX_SIG */'])
assert old2 in s, 'trap entry'
s = s.replace(old2, new2, 1)

open(p, 'w', encoding='utf-8', newline=NL).write(s)
print('perf trap patch applied')
