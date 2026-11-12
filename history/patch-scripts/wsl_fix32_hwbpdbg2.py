NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_DBG2' in s:
    print('already')
else:
    old = NL.join([
'    while (page <= end)',
'    {',
'        set_page_vprot_bits( page, page_size, VPROT_WRITEWATCH, 0 );',
'        mprotect_range( page, page_size, 0, 0 );',
'        page += page_size;',
'    }'])
    new = NL.join([
'    while (page <= end)',
'    {',
'        set_page_vprot_bits( page, page_size, VPROT_WRITEWATCH, 0 );',
'        mprotect_range( page, page_size, 0, 0 );',
'        { char hb[192]; int hn = snprintf( hb, sizeof(hb),',
'            "HWBP_DBG2 arm page=%lx vprot=%x unixprot=%x errno=%d' + BS + 'n",',
'            (unsigned long)page, (unsigned)get_page_vprot(page),',
'            (unsigned)get_unix_prot(get_page_vprot(page)), errno );',
'          write( 2, hb, hn ); }',
'        page += page_size;',
'    }'])
    assert old in s, 'arm loop'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched HWBP_DBG2')
