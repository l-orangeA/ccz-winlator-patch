import io
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
old = '''void hwbp_hold_begin( void ) { hwbp_hold_flag = 1; }'''
new = '''void hwbp_hold_begin( void )
{
    int i;
    hwbp_hold_flag = 1;
    for (i = 0; i < 8; i++)
    {
        char *pg = (char *)hwbp_rw_page[i];
        if (!pg) continue;
        set_page_vprot_bits( pg, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );
        mprotect_range( pg, page_size, 0, 0 );
    }
}'''
assert old in s, 'hold begin'
s = s.replace(old, new, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('hold begin unprotects')
