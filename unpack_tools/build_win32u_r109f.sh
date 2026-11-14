#!/bin/bash
# r109f: 带 freetype 重配 win32u（修复 --without-freetype 导致的 GUI 初始化崩溃）。
# 1) ./configure 加 --with-freetype（其余 --without 保留）
# 2) 对 win32u 编译命令注入 -DNDEBUG（安全处理续行）
# 3) 重编 + 验证符号签名（无 __assert_fail；有 dlopen/open/mmap —— 与原版一致）
set -e
cd /root/wine0905

./configure --enable-archs=i386,x86_64 --with-freetype \
  --without-x --without-fontconfig --without-gnutls \
  --without-alsa --without-pulse --without-dbus --without-unwind \
  --without-vulkan --without-sdl --without-udev --without-krb5 --without-cups \
  --without-netapi --without-sane --without-usb --without-gphoto --without-gstreamer \
  --without-oss --without-v4l2 --without-capi --without-pcap > /root/cfg_ft.log 2>&1
echo "configure rc=$?"
grep -iE 'freetype' /root/cfg_ft.log | head -3

# NDEBUG 注入（安全版：续行命令把 -DNDEBUG 插到反斜杠之前）
python3 - <<'PYEOF'
import re
p = '/root/wine0905/Makefile'
lines = open(p, encoding='utf-8', errors='replace').read().split('\n')
out = []
n = 0
pat = re.compile(r'^\t\$\((i386_CC|x86_64_CC|CC)\) -c ')
for ln in lines:
    if pat.match(ln) and 'dlls/win32u/' in ln and 'NDEBUG' not in ln:
        r = ln.rstrip()
        if r.endswith('\\'):
            ln = r[:-1].rstrip() + ' -DNDEBUG \\'
        else:
            ln = r + ' -DNDEBUG'
        n += 1
    out.append(ln)
open(p, 'w', encoding='utf-8', newline='\n').write('\n'.join(out))
print('NDEBUG injected into', n, 'commands')
PYEOF

make -n dlls/win32u/dibdrv/primitives.o > /dev/null && echo 'Makefile OK'

rm -f dlls/win32u/*.o dlls/win32u/dibdrv/*.o dlls/win32u/i386-windows/*.o dlls/win32u/x86_64-windows/*.o
make -j8 dlls/win32u/win32u.so 2>&1 | tail -3

echo '=== 符号校验 ==='
echo -n '__assert_fail (want 0): '
readelf --dyn-syms -W dlls/win32u/win32u.so | grep -c '__assert_fail' || true
echo -n 'dlopen (want >=1): '
readelf --dyn-syms -W dlls/win32u/win32u.so | grep -c ' dlopen' || true
echo -n 'mmap (want >=1): '
readelf --dyn-syms -W dlls/win32u/win32u.so | grep -c ' mmap' || true

cp dlls/win32u/win32u.so /mnt/d/ccz_winlator_patch/build/win32u_r109f.so
md5sum /mnt/d/ccz_winlator_patch/build/win32u_r109f.so
ls -la /mnt/d/ccz_winlator_patch/build/win32u_r109f.so
