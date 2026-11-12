NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_RW' in s:
    print('already')
else:
    # track read/write watched pages
    old = 'static int hwbp_pending;  /* HWBP_V2: 0 none, 1 matched (deliver), 2 unmatched (suppress) */'
    new = NL.join([
'static int hwbp_pending;  /* HWBP_V2: 0 none, 1 matched (deliver), 2 unmatched (suppress) */',
'static unsigned long hwbp_rw_page[4];  /* HWBP_RW: pages watched for read+write (PROT_NONE) */'])
    assert old in s
    s = s.replace(old, new, 1)

    # arm: rw==3 -> PROT_NONE + remember page
    old = NL.join([
'static void hwbp_arm_one( unsigned long a, unsigned int len )',
'{',
'    char *page, *end;',
'    unsigned int size = hwbp_len_bytes( len );',
'    if (!a) return;',
'    page = (char *)(a & ~(unsigned long)page_mask);',
'    end = (char *)((a + size - 1) & ~(unsigned long)page_mask);',
'    while (page <= end)',
'    {'])
    new = NL.join([
'static void hwbp_arm_one( unsigned long a, unsigned int len, unsigned int rw )',
'{',
'    char *page, *end;',
'    unsigned int size = hwbp_len_bytes( len );',
'    int i;',
'    if (!a) return;',
'    page = (char *)(a & ~(unsigned long)page_mask);',
'    end = (char *)((a + size - 1) & ~(unsigned long)page_mask);',
'    while (page <= end)',
'    {',
'        if (rw == 3)',
'        {',
'            /* read/write watch: remove access entirely */',
'            set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );',
'            mprotect_range( page, page_size, 0, 0 );',
'            for (i = 0; i < 4; i++) if (!hwbp_rw_page[i]) { hwbp_rw_page[i] = (unsigned long)page; break; }',
'            page += page_size;',
'            continue;',
'        }'])
    assert old in s, 'arm one'
    s = s.replace(old, new, 1)

    # callers pass rw
    s = s.replace('hwbp_arm_one( hwbp_dr[i], len );', 'hwbp_arm_one( hwbp_dr[i], len, rw );')

    # pfn: is page in rw watch list
    old = 'int hwbp_take_pending(void) { int v = hwbp_pending; hwbp_pending = 0; return v; }'
    new = NL.join([
'int hwbp_take_pending(void) { int v = hwbp_pending; hwbp_pending = 0; return v; }',
'',
'static int hwbp_rw_watched( char *page )',
'{',
'    int i;',
'    for (i = 0; i < 4; i++) if (hwbp_rw_page[i] == (unsigned long)page) return 1;',
'    return 0;',
'}',
'',
'/* HWBP_RW: called when a fault hits a read/write watched page */',
'void hwbp_handle_rw_fault( char *page )',
'{',
'    hwbp_pending = 1;   /* caller re-checks address match */',
'    hwbp_step_flag = 1;',
'    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, 0 );',
'    mprotect_range( page, page_size, 0, 0 );',
'}'])
    assert old in s
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('virtual patched HWBP_RW')
