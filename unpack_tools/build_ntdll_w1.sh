#!/bin/bash
# W1：restore_context 对 32 位 guest 跳过 Dr 覆盖（+ RCTX32 取证日志）
# 在 WSL 跑: bash /mnt/d/ccz_winlator_patch/unpack_tools/build_ntdll_w1.sh
set -e
WINE=/root/wine0905
TOOLS=/mnt/d/ccz_winlator_patch/unpack_tools
OUT=/mnt/d/ccz_winlator_patch/build
V=$WINE/dlls/ntdll/unix/virtual.c
S=$WINE/dlls/ntdll/unix/signal_x86_64.c
BK=/root/ntdll_w1_backup_20261108

mkdir -p "$BK"
cp "$V" "$BK/virtual.c"
cp "$S" "$BK/signal_x86_64.c"
restore_src() { cp "$BK/virtual.c" "$V"; cp "$BK/signal_x86_64.c" "$S"; touch "$V" "$S"; }
trap restore_src EXIT

echo "=== 1/3 应用 fix110c + W1 ==="
python3 "$TOOLS/patch_fix110c.py"
python3 - <<'PY'
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = """static void restore_context( const struct xcontext *xcontext, ucontext_t *sigcontext )
{
    const CONTEXT *context = &xcontext->c;
    XSTATE *xs;

    amd64_thread_data()->dr0 = context->Dr0;
    amd64_thread_data()->dr1 = context->Dr1;
    amd64_thread_data()->dr2 = context->Dr2;
    amd64_thread_data()->dr3 = context->Dr3;
    amd64_thread_data()->dr6 = context->Dr6;
    amd64_thread_data()->dr7 = context->Dr7;"""
new = """static void restore_context( const struct xcontext *xcontext, ucontext_t *sigcontext )
{
    const CONTEXT *context = &xcontext->c;
    XSTATE *xs;

    /* W1: a 32-bit guest CONTEXT is I386_CONTEXT (Dr1 @0x08, FloatSave @0x1c),
       not the 64-bit layout this code reads (Dr1 @~0x40). Reading it here
       lands in FloatSave and poisons amd64_thread_data()->dr* with garbage
       (observed dr1=ffce115f), so hwbp never matches. For 32-bit frames keep
       whatever NtSetContextThread wrote. */
    if (CS_sig(sigcontext) != 0x23) {
        amd64_thread_data()->dr0 = context->Dr0;
        amd64_thread_data()->dr1 = context->Dr1;
        amd64_thread_data()->dr2 = context->Dr2;
        amd64_thread_data()->dr3 = context->Dr3;
        amd64_thread_data()->dr6 = context->Dr6;
        amd64_thread_data()->dr7 = context->Dr7;
    } else {
        static int w1n;
        if (w1n++ < 200) {
            FILE *fp = fopen("/home/xuser/w1_probe.log", "a");
            if (fp) { fprintf(fp, "RCTX32 dr0=%lx dr1=%lx dr7=%lx cs=%lx\\n",
                (unsigned long)context->Dr0, (unsigned long)context->Dr1,
                (unsigned long)context->Dr7, (unsigned long)CS_sig(sigcontext)); fclose(fp); }
        }
    }"""
assert s.count(old) == 1, 'anchor count=%d' % s.count(old)
open(p, 'w', encoding='utf-8', newline='\n').write(s.replace(old, new, 1))
print('W1 applied')
PY
touch "$V" "$S"

echo "=== 2/3 编译 ntdll.so ==="
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_w1.log 2>&1 || { tail -40 /root/make_w1.log; exit 1; }
tail -3 /root/make_w1.log
M=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  W1 md5=$M"
cp "$WINE/dlls/ntdll/ntdll.so" "$OUT/ntdll_w1.so"

echo "=== 3/3 复原源码并重编 ==="
restore_src
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_w1_restore.log 2>&1 || { tail -30 /root/make_w1_restore.log; exit 1; }
R=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  restored md5=$R"
trap - EXIT
echo "PASS: build/ntdll_w1.so 已产出，源码复原"
