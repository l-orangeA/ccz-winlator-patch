NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
if 'FORCE_HASL' in s:
    print('already')
else:
    old = '    return (void *)((addr << 1) - (granularity_mask + 1));'
    new = NL.join([
'    /* FORCE_HASL: WSL probe under-reports; use the architectural 47-bit limit */',
'    if (1) return (void *)0x7ffffffff000;',
'    return (void *)((addr << 1) - (granularity_mask + 1));'])
    assert old in s
    s = s.replace(old, new, 1)
    open(p, 'w', encoding='utf-8', newline=NL).write(s)
    print('patched FORCE_HASL')
