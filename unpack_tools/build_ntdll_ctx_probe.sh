#!/bin/bash
# 编译 ntdll 上下文探针：fix110c + 64 位 NtSetContextThread 入口 WCTX64 日志（前 400 次）
# 在 WSL 跑: bash /mnt/d/ccz_winlator_patch/unpack_tools/build_ntdll_ctx_probe.sh
set -e
WINE=/root/wine0905
TOOLS=/mnt/d/ccz_winlator_patch/unpack_tools
OUT=/mnt/d/ccz_winlator_patch/build
V=$WINE/dlls/ntdll/unix/virtual.c
S=$WINE/dlls/ntdll/unix/signal_x86_64.c
BK=/root/ntdll_ctx_probe_backup_20261108

mkdir -p "$BK"
cp "$V" "$BK/virtual.c"
cp "$S" "$BK/signal_x86_64.c"
restore_src() { cp "$BK/virtual.c" "$V"; cp "$BK/signal_x86_64.c" "$S"; touch "$V" "$S"; }
trap restore_src EXIT

echo "=== 1/3 应用 fix110c + WCTX64 探针 ==="
python3 "$TOOLS/patch_fix110c.py"
python3 - <<'PY'
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = """NTSTATUS WINAPI NtSetContextThread( HANDLE handle, const CONTEXT *context )
{
    NTSTATUS ret = STATUS_SUCCESS;
    DWORD flags = context->ContextFlags & ~CONTEXT_AMD64;"""
new = """NTSTATUS WINAPI NtSetContextThread( HANDLE handle, const CONTEXT *context )
{
    NTSTATUS ret = STATUS_SUCCESS;
    DWORD flags = context->ContextFlags & ~CONTEXT_AMD64;
    { static int n64; if (n64++ < 400) { FILE *fp = fopen("/home/xuser/ctx_probe.log", "a");
      if (fp) { fprintf(fp, "WCTX64 ctx=%p flags=%lx rip=%lx rsp=%lx dr0=%lx dr1=%lx dr7=%lx\\n",
        (void*)context, (unsigned long)context->ContextFlags, (unsigned long)context->Rip,
        (unsigned long)context->Rsp, (unsigned long)context->Dr0, (unsigned long)context->Dr1,
        (unsigned long)context->Dr7); fclose(fp); } } }"""
assert s.count(old) == 1, 'anchor count=%d' % s.count(old)
open(p, 'w', encoding='utf-8', newline='\n').write(s.replace(old, new, 1))
print('WCTX64 inserted')
PY
touch "$V" "$S"

echo "=== 2/3 编译 ntdll.so ==="
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_ctx_probe.log 2>&1 || { tail -40 /root/make_ctx_probe.log; exit 1; }
tail -3 /root/make_ctx_probe.log
M=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  ctx-probe md5=$M"
cp "$WINE/dlls/ntdll/ntdll.so" "$OUT/ntdll_ctx_probe.so"

echo "=== 3/3 复原源码并重编 ==="
restore_src
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_ctx_restore.log 2>&1 || { tail -30 /root/make_ctx_restore.log; exit 1; }
R=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  restored md5=$R"
trap - EXIT
echo "PASS: build/ntdll_ctx_probe.so 已产出，源码复原"
