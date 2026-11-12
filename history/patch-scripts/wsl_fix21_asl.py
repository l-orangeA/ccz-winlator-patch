NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'FORCE_ASL' in s:
    print('already')
else:
    old = NL.join([
'    host_addr_space_limit = address_space_limit;',
'#endif',
'',
'    if (preload_info && *preload_info)'])
    new = NL.join([
'    host_addr_space_limit = address_space_limit;',
'#endif',
'    /* FORCE_ASL: keep vprot coverage above any host mmap placement (WSL/5-level) */',
'    host_addr_space_limit = (void *)0x7ffffffff000;',
'',
'    if (preload_info && *preload_info)'])
    assert old in s, 'asl block'
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched FORCE_ASL')
