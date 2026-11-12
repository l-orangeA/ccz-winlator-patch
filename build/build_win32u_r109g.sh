#!/bin/bash
# r109g: 在 r109f 基础上加 --with-fontconfig（对齐原版功能集——fontconfig 负责字体族匹配）。
# 前置: primitives.c 保持 pristine（无 memset 补丁）。验证 Fc* 符号与 libfontconfig.so.1 字样。
set -e
cd /root/wine0905

./configure --enable-archs=i386,x86_64 --with-freetype --with-fontconfig \
  --without-x --without-gnutls \
  --without-alsa --without-pulse --without-dbus --without-unwind \
  --without-vulkan --without-sdl --without-udev --without-krb5 --without-cups \
  --without-netapi --without-sane --without-usb --without-gphoto --without-gstreamer \
  --without-oss --without-v4l2 --without-capi --without-pcap > /root/cfg_fc.log 2>&1
echo "configure rc=$?"
grep -iE 'fontconfig' /root/cfg_fc.log | head -3

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

echo '=== 校验 ==='
echo -n '__assert_fail (want 0): '
readelf --dyn-syms -W dlls/win32u/win32u.so | grep -c '__assert_fail' || true
echo -n 'FcInit/Fc 字符串 (want >=8): '
strings -a dlls/win32u/win32u.so | grep -cE 'Fc(Init|Config|Font|Pattern)' || true
echo -n 'libfontconfig 字样: '
strings -a dlls/win32u/win32u.so | grep -c 'libfontconfig.so.1' || true

cp dlls/win32u/win32u.so /mnt/d/ccz_winlator_patch/build/win32u_r109g.so
md5sum /mnt/d/ccz_winlator_patch/build/win32u_r109g.so
