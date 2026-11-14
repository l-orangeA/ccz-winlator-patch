#!/bin/bash
# 编译 ntdll 探针版：fix110c + hwbp_update 调用点日志（只前 300 次）
# 在 WSL 跑: bash /mnt/d/ccz_winlator_patch/unpack_tools/build_ntdll_hwbp_probe.sh
set -e
WINE=/root/wine0905
TOOLS=/mnt/d/ccz_winlator_patch/unpack_tools
OUT=/mnt/d/ccz_winlator_patch/build
V=$WINE/dlls/ntdll/unix/virtual.c
BK=/root/ntdll_probe_backup_20261108

mkdir -p "$BK"
cp "$V" "$BK/virtual.c"
restore_src() { cp "$BK/virtual.c" "$V"; touch "$V"; }
trap restore_src EXIT

echo "=== 1/3 应用 fix110c + 调用点探针 ==="
python3 "$TOOLS/patch_fix110c.py"
python3 - <<'PY'
p = '/root/wine0905/dlls/ntdll/unix/virtual.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = """void hwbp_update( unsigned long d0, unsigned long d1, unsigned long d2, unsigned long d3, unsigned long d7 )
{
    int i;"""
new = """void hwbp_update( unsigned long d0, unsigned long d1, unsigned long d2, unsigned long d3, unsigned long d7 )
{
    int i;
    if (hwbp_verbose()) { static int pn; if (pn++ < 300) { char b[160]; int n = snprintf(b,sizeof(b),"HWBP_CALLER ret=%p d1=%lx d7=%lx\\n", __builtin_return_address(0), d1, d7); write(2,b,n); } }"""
assert s.count(old) == 1, 'anchor count=%d' % s.count(old)
open(p, 'w', encoding='utf-8', newline='\n').write(s.replace(old, new, 1))
print('probe inserted')
PY
touch "$V"

echo "=== 2/3 编译 ntdll.so ==="
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_hwbp_probe.log 2>&1 || { tail -40 /root/make_hwbp_probe.log; exit 1; }
tail -3 /root/make_hwbp_probe.log
M=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  probe md5=$M"
cp "$WINE/dlls/ntdll/ntdll.so" "$OUT/ntdll_hwbp_probe.so"

echo "=== 3/3 复原源码并重编 ==="
restore_src
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_hwbp_restore.log 2>&1 || { tail -30 /root/make_hwbp_restore.log; exit 1; }
R=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  restored md5=$R"
trap - EXIT
echo "PASS: build/ntdll_hwbp_probe.so 已产出，源码复原（产物 md5 可能因增量构建变化，部署以探针版为准）"
