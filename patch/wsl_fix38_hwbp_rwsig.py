NL = chr(10)
BS = chr(92)

# ---- virtual.c: forward decl ----
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_FWD' in s:
    print('virt fwd already')
else:
    old = '/* HWBP_RW: called when a fault hits a read/write watched page */'
    new = NL.join([
'static int hwbp_match_write( const void *addr );  /* HWBP_FWD */',
'',
'/* HWBP_RW: called when a fault hits a read/write watched page */'])
    assert old in s
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('fwd decl added')

# ---- signal_x86_64.c: hook rw faults ----
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
if 'HWBP_RW_SIG' in s2:
    print('signal already')
else:
    old = 'extern int hwbp_take_pending(void);  /* HWBP_V2_SIG */'
    new = NL.join([
'extern int hwbp_take_pending(void);  /* HWBP_V2_SIG */',
'extern char *hwbp_rw_page_for( const void *addr );  /* HWBP_RW_SIG */',
'extern void hwbp_handle_rw_fault( char *page, const void *addr );'])
    assert old in s2
    s2 = s2.replace(old, new, 1)

    old = NL.join([
'    case TRAP_x86_PAGEFLT:  /* Page fault */',
'        rec.NumberParameters = 2;'])
    new = NL.join([
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
'        rec.NumberParameters = 2;'])
    assert old in s2, 'pagefault case'
    s2 = s2.replace(old, new, 1)
    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('signal patched HWBP_RW')
