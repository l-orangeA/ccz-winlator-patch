NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'hwbp_rw_page_for' in s:
    print('already')
else:
    old = 'extern int hwbp_take_pending(void);'
    assert old in s, 'extern'
    s = s.replace(old, NL.join([
old,
'extern char *hwbp_rw_page_for( const void *addr );',
'extern void hwbp_handle_rw_fault( char *page, const void *addr );']), 1)

    old2 = NL.join([
'    case TRAP_x86_PAGEFLT:  /* Page fault */',
'        if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000'])
    new2 = NL.join([
'    case TRAP_x86_PAGEFLT:  /* Page fault */',
'        {',
'            char *rwpage = hwbp_rw_page_for( siginfo->si_addr );',
'            if (rwpage)',
'            {',
'                hwbp_handle_rw_fault( rwpage, siginfo->si_addr );',
'                EFL_sig(ucontext) |= 0x100;',
'                leave_handler( ucontext );',
'                return;',
'            }',
'        }',
'        if ((ULONG_PTR)RIP_sig(ucontext) < 0x10000000'])
    assert old2 in s, 'case'
    s = s.replace(old2, new2, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('hook added')
