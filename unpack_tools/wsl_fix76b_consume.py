import io
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
anchor = 'void hwbp_hold_end( void ) { hwbp_hold_flag = 0; hwbp_reprotect_all(); }'
assert anchor in s, 'hold_end'
add = '''/* HWBP_CONSUME: the watch fired; leave pages open until the next arm */
void hwbp_consume( void )
{
    int i;
    for (i = 0; i < 8; i++)
    {
        char *pg = (char *)hwbp_rw_page[i];
        if (!pg) continue;
        hwbp_rw_page[i] = 0;
        set_page_vprot_bits( pg, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );
        mprotect_range( pg, page_size, 0, 0 );
    }
    hwbp_tempbp_clear_all();
}
'''
s = s.replace(anchor, anchor + '\n\n' + add, 1)
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('consume added')
