NL = chr(10)
BS = chr(92)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'HASL_DBG' in s:
    print('already')
else:
    old = '    pages_vprot_size = ((size_t)host_addr_space_limit >> page_shift >> pages_vprot_shift) + 1;'
    new = NL.join([
'    pages_vprot_size = ((size_t)host_addr_space_limit >> page_shift >> pages_vprot_shift) + 1;',
'    { char hb[128]; int hn = snprintf( hb, sizeof(hb),',
'      "HASL_DBG limit=%lx pvsize=%lx' + BS + 'n",',
'      (unsigned long)host_addr_space_limit, (unsigned long)pages_vprot_size ); write( 2, hb, hn ); }'])
    assert old in s
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched HASL_DBG')
