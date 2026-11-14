#!/bin/bash
# r109d-v3: 修复 v1 对续行命令的破坏（' \ -DNDEBUG' -> ' -DNDEBUG \'），保留合法追加，重编验证。
set -e
cd /root/wine0905

python3 - <<'PYEOF'
p = '/root/wine0905/Makefile'
lines = open(p, encoding='utf-8', errors='replace').read().split('\n')
out = []
fixed = 0
for ln in lines:
    if ln.rstrip('\r') != ln:
        pass
    r = ln.rstrip()
    if r.endswith('\\ -DNDEBUG'):
        # v1 破坏的续行：把 -DNDEBUG 放回反斜杠之前
        head = r[:-len('\\ -DNDEBUG')].rstrip()
        ln = head + ' -DNDEBUG \\'
        fixed += 1
    out.append(ln)
open(p, 'w', encoding='utf-8', newline='\n').write('\n'.join(out))
print('continuation lines repaired:', fixed)
PYEOF

echo '=== make parse check ==='
make -n dlls/win32u/dibdrv/primitives.o > /dev/null && echo 'Makefile OK'
make -n dlls/win32u/bitblt.o > /dev/null && echo 'bitblt rule OK'

echo '=== rebuild ==='
rm -f dlls/win32u/*.o dlls/win32u/dibdrv/*.o dlls/win32u/i386-windows/*.o dlls/win32u/x86_64-windows/*.o
make -j8 dlls/win32u/win32u.so 2>&1 | tail -3

echo '=== __assert_fail references ==='
readelf --dyn-syms -W dlls/win32u/win32u.so | grep '__assert_fail' && echo 'STILL HAS ASSERT' || echo 'clean'

cp dlls/win32u/win32u.so /mnt/d/ccz_winlator_patch/build/win32u_r109d.so
md5sum /mnt/d/ccz_winlator_patch/build/win32u_r109d.so
ls -la /mnt/d/ccz_winlator_patch/build/win32u_r109d.so
