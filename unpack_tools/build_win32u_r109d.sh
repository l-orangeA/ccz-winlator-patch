#!/bin/bash
# r109d-v2: 先恢复 Makefile（config.status 重新生成），再精确注入 -DNDEBUG 到 win32u 编译命令。
set -e
cd /root/wine0905

# 1) 恢复干净 Makefile
./config.status Makefile
grep -q 'WIN32U_NDEBUG_MARKER' Makefile && echo 'WARN: marker still present' || echo 'Makefile regenerated clean'

# 2) 精确注入：只匹配真正的编译命令行 $(CC) / $(i386_CC) / $(x86_64_CC) 开头 + -c + dlls/win32u/
python3 - <<'PYEOF'
import re
p = '/root/wine0905/Makefile'
lines = open(p, encoding='utf-8', errors='replace').read().split('\n')
out = []
n = 0
pat = re.compile(r'^\t\$\((i386_CC|x86_64_CC|CC)\) -c ')
for ln in lines:
    if pat.match(ln) and 'dlls/win32u/' in ln and not ln.rstrip().endswith('-DNDEBUG'):
        ln = ln.rstrip() + ' -DNDEBUG'
        n += 1
    out.append(ln)
open(p, 'w', encoding='utf-8', newline='\n').write('\n'.join(out))
print('patched compile commands:', n)
PYEOF
echo '# WIN32U_NDEBUG_MARKER r109d' >> Makefile

# 3) 验证 Makefile 可解析
make -n dlls/win32u/dibdrv/primitives.o >/dev/null && echo 'Makefile OK'

# 4) 强制重编
rm -f dlls/win32u/*.o dlls/win32u/dibdrv/*.o dlls/win32u/i386-windows/*.o dlls/win32u/x86_64-windows/*.o
make -j8 dlls/win32u/win32u.so 2>&1 | tail -3

echo '=== __assert_fail references (want clean) ==='
readelf --dyn-syms -W dlls/win32u/win32u.so | grep '__assert_fail' && echo 'STILL HAS ASSERT' || echo 'clean'

cp dlls/win32u/win32u.so /mnt/d/ccz_winlator_patch/build/win32u_r109d.so
md5sum /mnt/d/ccz_winlator_patch/build/win32u_r109d.so
ls -la /mnt/d/ccz_winlator_patch/build/win32u_r109d.so
