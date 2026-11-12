NL = chr(10)
BS = chr(92)

# ---- virtual.c: expose helpers ----
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_RW2' in s:
    print('virt already')
else:
    old = NL.join([
'/* HWBP_RW: called when a fault hits a read/write watched page */',
'void hwbp_handle_rw_fault( char *page )',
'{',
'    hwbp_pending = 1;   /* caller re-checks address match */',
'    hwbp_step_flag = 1;',
'    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, 0 );',
'    mprotect_range( page, page_size, 0, 0 );',
'}'])
    new = NL.join([
'/* HWBP_RW: called when a fault hits a read/write watched page */',
'void hwbp_handle_rw_fault( char *page, const void *addr )',
'{',
'    hwbp_pending = hwbp_match_write( addr ) ? 1 : 2;',
'    hwbp_step_flag = 1;',
'    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, 0 );',
'    mprotect_range( page, page_size, 0, 0 );',
'}   /* HWBP_RW2 */',
'',
'char *hwbp_rw_page_for( const void *addr )',
'{',
'    char *page = (char *)((ULONG_PTR)addr & ~(ULONG_PTR)page_mask);',
'    return hwbp_rw_watched( page ) ? page : NULL;',
'}'])
    assert old in s, 'rw fault fn'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('virt patched')
