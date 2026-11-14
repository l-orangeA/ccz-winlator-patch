NL = chr(10)
BS = chr(92)

p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = '    assert( end <= pages_vprot_size << pages_vprot_shift );'
if 'VPROT_ASSERT' in s:
    print('already')
else:
    new = NL.join([
'    if (end > pages_vprot_size << pages_vprot_shift)',
'    {',
'        char vb[160]; int vn = snprintf( vb, sizeof(vb),',
'            "VPROT_ASSERT addr=%lx size=%lx end=%lx cover=%lx' + BS + 'n",',
'            (unsigned long)addr, (unsigned long)size, (unsigned long)end,',
'            (unsigned long)(pages_vprot_size << pages_vprot_shift) );',
'        write( 2, vb, vn );',
'        return FALSE;',
'    }'])
    assert old in s
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline='\n').write(s)
    print('patched VPROT_ASSERT')
