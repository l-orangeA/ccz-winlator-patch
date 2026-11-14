import io
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = io.open(p, encoding='utf-8', errors='replace', newline='').read()
b1 = '"HWBP_PERF software fd=%d errno=%d\n", sfd, e1'
g1 = '"HWBP_PERF software fd=%d errno=%d\\n", sfd, e1'
b2 = '"HWBP_PERF bp-noxk fd=%d errno=%d\n", bfd, e2'
g2 = '"HWBP_PERF bp-noxk fd=%d errno=%d\\n", bfd, e2'
n = 0
for b, g in ((b1, g1), (b2, g2)):
    if b in s:
        s = s.replace(b, g)
        n += 1
io.open(p, 'w', encoding='utf-8', newline='').write(s)
print('fixed', n)
