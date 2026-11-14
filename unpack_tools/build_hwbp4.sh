#!/bin/bash
# hwbp4 = hwbp3 + stderr freopen 到 HOME/wine_dbg.log（让 WINEDEBUG 输出落盘）
set -e
WINE=/root/wine1010
OUT=/mnt/d/ccz_winlator_patch/build
V=$WINE/dlls/ntdll/unix/virtual.c

echo "=== 1/2 加 stderr 重定向 constructor ==="
python3 - <<'PY'
p = '/root/wine1010/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
anchor = '/* HWBP_SOFT: software emulation of x86 hardware breakpoints (ARM hosts) */'
assert s.count(anchor) == 1, 'anchor=%d' % s.count(anchor)
ctor = '''__attribute__((constructor)) static void wine_dbg_redirect( void )
{
    const char *h = getenv( "HOME" );
    char pth[384];
    if (!getenv( "WINE_DBG_LOG" )) return;
    snprintf( pth, sizeof(pth), "%s/wine_dbg.log", h ? h : "/data/data/com.winlator/files" );
    freopen( pth, "w", stderr );
}

'''
s = s.replace(anchor, ctor + anchor, 1)
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('constructor added (gated by WINE_DBG_LOG)')
PY

echo "=== 2/2 编译 ntdll.so ==="
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_hwbp4.log 2>&1 || { tail -40 /root/make_hwbp4.log; exit 1; }
tail -2 /root/make_hwbp4.log
M=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  hwbp4 md5=$M"
cp "$WINE/dlls/ntdll/ntdll.so" "$OUT/ntdll_wine10_hwbp4.so"
echo "PASS"
