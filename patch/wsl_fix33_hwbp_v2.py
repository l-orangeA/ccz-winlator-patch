NL = chr(10)
BS = chr(92)

# ---- virtual.c: track pending step and page watch ----
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_V2' in s:
    print('virtual already')
else:
    old = 'static int hwbp_step_flag;'
    new = NL.join([
'static int hwbp_step_flag;',
'static int hwbp_pending;  /* HWBP_V2: 0 none, 1 matched (deliver), 2 unmatched (suppress) */'])
    assert old in s
    s = s.replace(old, new, 1)

    # helper: is page watched
    old = 'static void hwbp_arm_one( unsigned long a, unsigned int len )'
    new = NL.join([
'static int hwbp_page_watched( char *page )',
'{',
'    int i;',
'    if (!(hwbp_dr7 & 0xff)) return 0;',
'    for (i = 0; i < 4; i++)',
'    {',
'        unsigned int rw;',
'        unsigned long pa;',
'        if (!(hwbp_dr7 & (1u << (2 * i))) && !(hwbp_dr7 & (2u << (2 * i)))) continue;',
'        rw = (unsigned int)((hwbp_dr7 >> (16 + 4 * i)) & 3);',
'        if (rw == 0 || !hwbp_dr[i]) continue;',
'        pa = hwbp_dr[i] & ~(unsigned long)page_mask;',
'        if (pa == (unsigned long)page) return 1;',
'    }',
'    return 0;',
'}',
'',
'int hwbp_take_pending(void) { int v = hwbp_pending; hwbp_pending = 0; return v; }',
'',
'static void hwbp_arm_one( unsigned long a, unsigned int len )'])
    assert old in s
    s = s.replace(old, new, 1)

    # in writewatch branch: set pending for both match and non-match on watched pages
    old = NL.join([
'            if (hwbp_match_write( addr ))',
'            {',
'                hwbp_step_flag = 1;   /* HWBP_SOFT: trap after this store */',
'            }'])
    new = NL.join([
'            if (hwbp_page_watched( page ))',
'            {',
'                hwbp_pending = hwbp_match_write( addr ) ? 1 : 2;',
'                hwbp_step_flag = 1;   /* HWBP_V2: trap after this store */',
'            }'])
    assert old in s
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('virtual patched HWBP_V2')

# ---- signal_x86_64.c: suppress non-matching steps ----
p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
if 'HWBP_V2_SIG' in s2:
    print('signal already')
else:
    old = 'extern int hwbp_step_pending(void);'
    new = NL.join([
'extern int hwbp_step_pending(void);',
'extern int hwbp_take_pending(void);  /* HWBP_V2_SIG */'])
    assert old in s2
    s2 = s2.replace(old, new, 1)

    old = NL.join([
'static void trap_handler( int signal, siginfo_t *siginfo, void *sigcontext )',
'{',
'    EXCEPTION_RECORD rec = { 0 };',
'    struct xcontext context;',
'    ucontext_t *ucontext = init_handler( sigcontext );'])
    new = NL.join([
'static void trap_handler( int signal, siginfo_t *siginfo, void *sigcontext )',
'{',
'    EXCEPTION_RECORD rec = { 0 };',
'    struct xcontext context;',
'    ucontext_t *ucontext = init_handler( sigcontext );',
'',
'    if (TRAP_sig(ucontext) == TRAP_x86_TRCTRAP)',
'    {',
'        int pend = hwbp_take_pending();',
'        if (pend == 2)          /* HWBP_V2: our own step, no match -> swallow */',
'        {',
'            hwbp_rearm();',
'            leave_handler( ucontext );',
'            return;',
'        }',
'        if (pend == 1) hwbp_rearm();   /* deliver the emulated breakpoint */',
'    }'])
    assert old in s2, 'trap handler prologue'
    s2 = s2.replace(old, new, 1)
    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('signal patched HWBP_V2')
