NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_ARMDBG' in s:
    print('already')
else:
    old = NL.join([
'        if (rw == 3)',
'        {',
'            set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );',
'            mprotect_range( page, page_size, 0, 0 );'])
    new = NL.join([
'        if (rw == 3)',
'        {',
'            set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );',
'            mprotect_range( page, page_size, 0, 0 );',
'            { char hb[160]; int hn = snprintf( hb, sizeof(hb), "HWBP_ARMDBG rw3 page=%lx vprot=%x prot=%x' + BS + 'n", (unsigned long)page, (unsigned)get_page_vprot(page), (unsigned)get_unix_prot(get_page_vprot(page)) ); write( 2, hb, hn ); }'])
    assert old in s
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('arm dbg added')

p2 = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s2 = open(p2, encoding='utf-8', errors='replace').read()
if 'HWBP_RWDBG' in s2:
    print('sig already')
else:
    old = '                int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );  /* DIRECT */'
    new = NL.join([
'                int idx = hwbp_handle_rw_fault( rwpage, siginfo->si_addr );  /* DIRECT */',
'                { char hb[160]; int hn = snprintf( hb, sizeof(hb), "HWBP_RWDBG addr=%lx idx=%d' + BS + 'n", (unsigned long)siginfo->si_addr, idx ); write( 2, hb, hn ); }  /* HWBP_RWDBG */'])
    assert old in s2
    s2 = s2.replace(old, new, 1)
    open(p2, 'w', encoding='utf-8', newline=NL).write(s2)
    print('rw dbg added')
