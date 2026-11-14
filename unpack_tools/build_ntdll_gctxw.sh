#!/bin/bash
# GCTXW：get_thread_wow64_context 写缓存前后取证（fix110c + 前 200 条）
# 在 WSL 跑: bash /mnt/d/ccz_winlator_patch/unpack_tools/build_ntdll_gctxw.sh
set -e
WINE=/root/wine0905
TOOLS=/mnt/d/ccz_winlator_patch/unpack_tools
OUT=/mnt/d/ccz_winlator_patch/build
V=$WINE/dlls/ntdll/unix/virtual.c
S=$WINE/dlls/ntdll/unix/signal_x86_64.c
BK=/root/ntdll_gctxw_backup_20261108

mkdir -p "$BK"
cp "$V" "$BK/virtual.c"
cp "$S" "$BK/signal_x86_64.c"
restore_src() { cp "$BK/virtual.c" "$V"; cp "$BK/signal_x86_64.c" "$S"; touch "$V" "$S"; }
trap restore_src EXIT

echo "=== 1/3 应用 fix110c + GCTXW ==="
python3 "$TOOLS/patch_fix110c.py"
python3 - <<'PY'
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = """        if (needed_flags & CONTEXT_I386_DEBUG_REGISTERS)
        {
            amd64_thread_data()->dr0 = context->Dr0;"""
new = """        if (needed_flags & CONTEXT_I386_DEBUG_REGISTERS)
        {
            { static int gn; if (gn++ < 200) { FILE *fp = fopen("/home/xuser/gctxw.log", "a");
              if (fp) { fprintf(fp, "GCTXW before=%lx ctx=%lx ret=%lx\\n",
                (unsigned long)amd64_thread_data()->dr1, (unsigned long)context->Dr1,
                (unsigned long)ret); fclose(fp); } } }
            amd64_thread_data()->dr0 = context->Dr0;"""
assert s.count(old) == 1, 'anchor count=%d' % s.count(old)
open(p, 'w', encoding='utf-8', newline='\n').write(s.replace(old, new, 1))
print('GCTXW inserted')
PY
touch "$V" "$S"

echo "=== 2/3 编译 ntdll.so ==="
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_gctxw.log 2>&1 || { tail -40 /root/make_gctxw.log; exit 1; }
tail -3 /root/make_gctxw.log
M=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  GCTXW md5=$M"
cp "$WINE/dlls/ntdll/ntdll.so" "$OUT/ntdll_gctxw.so"

echo "=== 3/3 复原源码并重编 ==="
restore_src
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_gctxw_restore.log 2>&1 || { tail -30 /root/make_gctxw_restore.log; exit 1; }
R=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  restored md5=$R"
trap - EXIT
echo "PASS: build/ntdll_gctxw.so 已产出，源码复原"
