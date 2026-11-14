NL = chr(10)
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
decl = 'static int hwbp_match_write( const void *addr );'
# remove all existing decls
s = s.replace(decl + NL, '')
# insert a single decl right after the rw page array
old = 'static unsigned long hwbp_rw_page[8];'
assert old in s
s = s.replace(old, old + NL + decl, 1)
open(p, 'w', encoding='utf-8', newline=NL).write(s)
print('fwd decl moved')
