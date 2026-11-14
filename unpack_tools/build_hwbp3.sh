#!/bin/bash
# hwbp3：hwbp_update + WRITEWATCH 命中路径加 HOME 文件日志（不依赖 stderr / destructor）
# 在 WSL 跑: bash /mnt/d/ccz_winlator_patch/unpack_tools/build_hwbp3.sh
set -e
WINE=/root/wine1010
OUT=/mnt/d/ccz_winlator_patch/build
V=$WINE/dlls/ntdll/unix/virtual.c

echo "=== 1/2 加文件日志 ==="
python3 - <<'PY'
p = '/root/wine1010/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()

old1 = '    if (hwbp_verbose()) { char hb[192]; int hn = snprintf( hb, sizeof(hb), "HWBP_DBG update dr0=%lx dr1=%lx dr2=%lx dr3=%lx dr7=%lx\\n", d0, d1, d2, d3, d7 ); write( 2, hb, hn ); }'
new1 = ('    if (hwbp_verbose()) { char hb[192]; int hn = snprintf( hb, sizeof(hb), "HWBP_DBG update dr0=%lx dr1=%lx dr2=%lx dr3=%lx dr7=%lx\\n", d0, d1, d2, d3, d7 ); write( 2, hb, hn );\n'
        '      const char *lh = getenv( "HOME" ); char lp[384]; snprintf( lp, sizeof(lp), "%s/hwbp_upd.log", lh ? lh : "/data/data/com.winlator/files" );\n'
        '      int lfd = open( lp, O_WRONLY | O_CREAT | O_APPEND, 0644 ); if (lfd >= 0) { write( lfd, hb, hn ); close( lfd ); } }')

old2 = '            if (hwbp_page_watched( page )) { int idx = hwbp_match_write( addr ); hwbp_pending = idx ? idx : 9; }'
new2 = ('            { int idx = hwbp_match_write( addr ); hwbp_pending = idx ? idx : 9;\n'
        '              const char *fh = getenv( "HOME" ); char fp[384]; snprintf( fp, sizeof(fp), "%s/hwbp_fault.log", fh ? fh : "/data/data/com.winlator/files" );\n'
        '              int ffd = open( fp, O_WRONLY | O_CREAT | O_APPEND, 0644 ); if (ffd >= 0) { char fb[160]; int fn = snprintf( fb, sizeof(fb), "FAULT page=%lx addr=%lx idx=%d\\n", (unsigned long)page, (unsigned long)addr, idx ); write( ffd, fb, fn ); close( ffd ); } }')

assert s.count(old1) == 1, 'anchor1 count=%d' % s.count(old1)
assert s.count(old2) == 1, 'anchor2 count=%d' % s.count(old2)
s = s.replace(old1, new1, 1).replace(old2, new2, 1)
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('file logging patched')
PY

echo "=== 2/2 编译 ntdll.so ==="
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_hwbp3.log 2>&1 || { tail -40 /root/make_hwbp3.log; exit 1; }
tail -2 /root/make_hwbp3.log
M=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  hwbp3 md5=$M"
cp "$WINE/dlls/ntdll/ntdll.so" "$OUT/ntdll_wine10_hwbp3.so"
echo "PASS: build/ntdll_wine10_hwbp3.so 已产出"
