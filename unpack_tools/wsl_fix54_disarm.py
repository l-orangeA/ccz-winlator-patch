NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()

# 1) add unwatch/clear helpers after hwbp_rw_watched
old = NL.join([
'static int hwbp_rw_watched( char *page )',
'{',
'    int i;',
'    for (i = 0; i < 8; i++) if (hwbp_rw_page[i] == (unsigned long)page) return 1;',
'    return 0;',
'}'])
new = NL.join([
old,
'',
'static void hwbp_rw_unwatch( char *page )',
'{',
'    int i;',
'    for (i = 0; i < 8; i++) if (hwbp_rw_page[i] == (unsigned long)page) hwbp_rw_page[i] = 0;',
'}',
'',
'static void hwbp_rw_clear( void )   /* HWBP_DISARM: restore protection of watched pages */',
'{',
'    int i;',
'    for (i = 0; i < 8; i++)',
'    {',
'        char *pg = (char *)hwbp_rw_page[i];',
'        if (!pg) continue;',
'        hwbp_rw_page[i] = 0;',
'        set_page_vprot_bits( pg, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );',
'        mprotect_range( pg, page_size, 0, 0 );',
'    }',
'}'])
assert old in s, 'rw_watched'
if 'HWBP_DISARM' not in s:
    s = s.replace(old, new, 1)

# 2) fault handler: drop the page from the list (it stays unprotected)
old2 = NL.join([
'    hwbp_pending = idx ? idx : 9;',
'    hwbp_step_flag = 0;   /* DIRECT */',
'    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, 0 );',
'    mprotect_range( page, page_size, 0, 0 );',
'    return idx;'])
new2 = NL.join([
'    hwbp_pending = idx ? idx : 9;',
'    hwbp_step_flag = 0;   /* DIRECT */',
'    hwbp_rw_unwatch( page );',
'    set_page_vprot_bits( page, page_size, VPROT_READ | VPROT_WRITE, VPROT_WRITEWATCH );',
'    mprotect_range( page, page_size, 0, 0 );',
'    return idx;'])
assert old2 in s, 'fault body'
s = s.replace(old2, new2, 1)

# 3) hwbp_arm_one: no duplicate entries
old3 = '            for (i = 0; i < 8; i++) if (!hwbp_rw_page[i]) { hwbp_rw_page[i] = (unsigned long)page; break; }'
new3 = '            if (!hwbp_rw_watched( page )) for (i = 0; i < 8; i++) if (!hwbp_rw_page[i]) { hwbp_rw_page[i] = (unsigned long)page; break; }'
assert old3 in s, 'arm entry'
s = s.replace(old3, new3, 1)

# 4) hwbp_update: disarm previous pages before applying the new state
old4 = NL.join([
'    hwbp_dr[0] = d0; hwbp_dr[1] = d1; hwbp_dr[2] = d2; hwbp_dr[3] = d3; hwbp_dr7 = d7;',
'    if (!(d7 & 0xff)) return;  /* no enabled breakpoints */'])
new4 = NL.join([
'    hwbp_dr[0] = d0; hwbp_dr[1] = d1; hwbp_dr[2] = d2; hwbp_dr[3] = d3; hwbp_dr7 = d7;',
'    hwbp_rw_clear();',
'    if (!(d7 & 0xff)) return;  /* no enabled breakpoints */'])
assert old4 in s, 'update body'
s = s.replace(old4, new4, 1)

open(p, 'w', encoding='utf-8', newline=NL).write(s)
print('disarm patch applied')
