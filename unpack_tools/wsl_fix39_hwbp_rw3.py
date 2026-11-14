import re
NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()

if 'hwbp_rw_page' in s:
    print('already has rw')
else:
    # 1) globals
    old = 'static int hwbp_pending;'
    assert old in s
    s = s.replace(old, old + '  /* rw watch pages below */' + NL +
                  'static unsigned long hwbp_rw_page[8];', 1)

    # 2) forward declaration of match
    old = 'static void hwbp_arm_one( unsigned long a, unsigned int len )'
    assert old in s
    s = s.replace(old, 'static int hwbp_match_write( const void *addr );' + NL + old, 1)

    # 3) rewrite arm_one body: add rw parameter and PROT_NONE handling
    start = s.index('static void hwbp_arm_one( unsigned long a, unsigned int len )')
    end = s.index('\n}\n', start) + 3
    newfn = NL.join([
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
'            set_page_vprot_bits( page, page_size, 0, VPROT_READ | VPROT_WRITE | VPROT_WRITEWATCH );',
'            mprotect_range( page, page_size, 0, 0 );',
'            for (i = 0; i < 8; i++) if (!hwbp_rw_page[i]) { hwbp_rw_page[i] = (unsigned long)page; break; }',
'        }',
'        else',
'        {',
'            set_page_vprot_bits( page, page_size, VPROT_WRITEWATCH, 0 );',
'            mprotect_range( page, page_size, 0, 0 );',
'        }',
'        page += page_size;',
'    }',
'}',
''])
    s = s[:start] + newfn + s[end:]

    # 4) call sites
    s = s.replace('hwbp_arm_one( hwbp_dr[i], len );', 'hwbp_arm_one( hwbp_dr[i], len, rw );')

    # 5) helpers after hwbp_take_pending
    anchor = 'int hwbp_take_pending(void) { int v = hwbp_pending; hwbp_pending = 0; return v; }'
    assert anchor in s
    helpers = NL.join([
anchor,
'',
'static int hwbp_rw_watched( char *page )',
'{',
'    int i;',
'    for (i = 0; i < 8; i++) if (hwbp_rw_page[i] == (unsigned long)page) return 1;',
'    return 0;',
'}',
'',
'char *hwbp_rw_page_for( const void *addr )',
'{',
'    char *page = (char *)((ULONG_PTR)addr & ~(ULONG_PTR)page_mask);',
'    return hwbp_rw_watched( page ) ? page : NULL;',
'}',
'',
'void hwbp_handle_rw_fault( char *page, const void *addr )',
'{',
'    hwbp_pending = hwbp_match_write( addr ) ? 1 : 2;',
'    hwbp_step_flag = 1;',
'    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, 0 );',
'    mprotect_range( page, page_size, 0, 0 );',
'}'])
    s = s.replace(anchor, helpers, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('rw watch added')
