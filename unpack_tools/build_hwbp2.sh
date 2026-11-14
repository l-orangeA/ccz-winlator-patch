#!/bin/bash
# hwbp2：stats 路径 HOME 化（v11.2 无 proot，/home/xuser 不存在）
# 在 WSL 跑: bash /mnt/d/ccz_winlator_patch/unpack_tools/build_hwbp2.sh
set -e
WINE=/root/wine1010
OUT=/mnt/d/ccz_winlator_patch/build
V=$WINE/dlls/ntdll/unix/virtual.c

echo "=== 1/2 修正 stats 路径 ==="
python3 - <<'PY'
p = '/root/wine1010/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = '    fd = open( "/home/xuser/hwbp_stats.txt", O_WRONLY | O_CREAT | O_TRUNC, 0644 );'
new = ('    { const char *h = getenv( "HOME" ); char pb[384];\n'
       '      snprintf( pb, sizeof(pb), "%s/hwbp_stats.txt", h ? h : "/data/data/com.winlator/files" );\n'
       '      fd = open( pb, O_WRONLY | O_CREAT | O_TRUNC, 0644 ); }')
assert s.count(old) == 1, 'anchor count=%d' % s.count(old)
open(p, 'w', encoding='utf-8', newline='\n').write(s.replace(old, new, 1))
print('stats path patched')
PY

echo "=== 2/2 编译 ntdll.so ==="
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_hwbp2.log 2>&1 || { tail -40 /root/make_hwbp2.log; exit 1; }
tail -2 /root/make_hwbp2.log
M=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  hwbp2 md5=$M"
cp "$WINE/dlls/ntdll/ntdll.so" "$OUT/ntdll_wine10_hwbp2.so"
echo "PASS: build/ntdll_wine10_hwbp2.so 已产出"
