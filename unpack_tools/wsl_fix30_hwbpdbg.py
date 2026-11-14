NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HWBP_DBG' in s:
    print('already')
else:
    old = 'void hwbp_update( unsigned long d0, unsigned long d1, unsigned long d2, unsigned long d3, unsigned long d7 )' + NL + '{' + NL + '    int i;'
    new = NL.join([
'void hwbp_update( unsigned long d0, unsigned long d1, unsigned long d2, unsigned long d3, unsigned long d7 )',
'{',
'    int i;',
'    { char hb[192]; int hn = snprintf( hb, sizeof(hb), "HWBP_DBG update dr0=%lx dr1=%lx dr2=%lx dr3=%lx dr7=%lx' + BS + 'n", d0, d1, d2, d3, d7 ); write( 2, hb, hn ); }'])
    assert old in s, 'hwbp update head'
    s = s.replace(old, new, 1)

    old2 = NL.join([
'            if (hwbp_match_write( addr ))',
'            {',
'                hwbp_step_flag = 1;   /* HWBP_SOFT: trap after this store */',
'            }'])
    new2 = NL.join([
'            { char hb[160]; int hn = snprintf( hb, sizeof(hb), "HWBP_DBG writewatch page=%lx addr=%lx match=%d' + BS + 'n", (unsigned long)page, (unsigned long)addr, hwbp_match_write( addr ) ); write( 2, hb, hn ); }',
'            if (hwbp_match_write( addr ))',
'            {',
'                hwbp_step_flag = 1;   /* HWBP_SOFT: trap after this store */',
'            }'])
    assert old2 in s, 'match block'
    s = s.replace(old2, new2, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched HWBP_DBG')
