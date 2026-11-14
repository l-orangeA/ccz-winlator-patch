#!/bin/bash
# hwbp5 = hwbp4 + wine_dbg 按进程 PID 分文件（消除多进程 freopen 覆盖）
set -e
WINE=/root/wine1010
OUT=/mnt/d/ccz_winlator_patch/build
V=$WINE/dlls/ntdll/unix/virtual.c

echo "=== 1/2 日志文件名加 PID ==="
python3 - <<'PY'
p = '/root/wine1010/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = '    snprintf( pth, sizeof(pth), "%s/wine_dbg.log", h ? h : "/data/data/com.winlator/files" );'
new = '    snprintf( pth, sizeof(pth), "%s/wine_dbg_%d.log", h ? h : "/data/data/com.winlator/files", (int)getpid() );'
assert s.count(old) == 1, 'anchor=%d' % s.count(old)
s = s.replace(old, new, 1)
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('pid-per-file patched')
PY

echo "=== 2/2 编译 ntdll.so ==="
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_hwbp5.log 2>&1 || { tail -40 /root/make_hwbp5.log; exit 1; }
tail -2 /root/make_hwbp5.log
M=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  hwbp5 md5=$M"
cp "$WINE/dlls/ntdll/ntdll.so" "$OUT/ntdll_wine10_hwbp5.so"
echo "PASS"
