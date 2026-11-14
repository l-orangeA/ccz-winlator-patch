#!/bin/bash
# DRF：Dr 白名单过滤（fix110c + 32 位 Get 返回前过滤越界 Dr1/Dr7）
# 在 WSL 跑: bash /mnt/d/ccz_winlator_patch/unpack_tools/build_ntdll_drf.sh
set -e
WINE=/root/wine0905
TOOLS=/mnt/d/ccz_winlator_patch/unpack_tools
OUT=/mnt/d/ccz_winlator_patch/build
V=$WINE/dlls/ntdll/unix/virtual.c
S=$WINE/dlls/ntdll/unix/signal_x86_64.c
BK=/root/ntdll_drf_backup_20261108

mkdir -p "$BK"
cp "$V" "$BK/virtual.c"
cp "$S" "$BK/signal_x86_64.c"
restore_src() { cp "$BK/virtual.c" "$V"; cp "$BK/signal_x86_64.c" "$S"; touch "$V" "$S"; }
trap restore_src EXIT

echo "=== 1/3 应用 fix110c + DRF ==="
python3 "$TOOLS/patch_fix110c.py"
python3 - <<'PY'
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = """        if (needed_flags & CONTEXT_I386_DEBUG_REGISTERS)
        {
            amd64_thread_data()->dr0 = context->Dr0;
            amd64_thread_data()->dr1 = context->Dr1;"""
new = """        if (needed_flags & CONTEXT_I386_DEBUG_REGISTERS)
        {
            /* DRF: 32-bit user space is < 0x80000000; observed garbage ffce115f
               is never set by the shell (WCTX/WCTX64/SETPROBE all show 0x31xxxx).
               Substitute the last known-good cached value before handing the
               CONTEXT back, so Get/Set/hwbp all see a legal address. */
            if ((uintptr_t)context->Dr1 >= 0x80000000UL)
                context->Dr1 = (DWORD)amd64_thread_data()->dr1;
            if ((uintptr_t)context->Dr7 >= 0x80000000UL)
                context->Dr7 = (DWORD)amd64_thread_data()->dr7;
            amd64_thread_data()->dr0 = context->Dr0;
            amd64_thread_data()->dr1 = context->Dr1;"""
assert s.count(old) == 1, 'anchor count=%d' % s.count(old)
open(p, 'w', encoding='utf-8', newline='\n').write(s.replace(old, new, 1))
print('DRF applied')
PY
touch "$V" "$S"

echo "=== 2/3 编译 ntdll.so ==="
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_drf.log 2>&1 || { tail -40 /root/make_drf.log; exit 1; }
tail -3 /root/make_drf.log
M=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  DRF md5=$M"
cp "$WINE/dlls/ntdll/ntdll.so" "$OUT/ntdll_drf.so"

echo "=== 3/3 复原源码并重编 ==="
restore_src
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_drf_restore.log 2>&1 || { tail -30 /root/make_drf_restore.log; exit 1; }
R=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  restored md5=$R"
trap - EXIT
echo "PASS: build/ntdll_drf.so 已产出，源码复原"
