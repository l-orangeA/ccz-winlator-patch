#!/bin/bash
# SETPROBE：只抓含 DEBUG_REGISTERS 位的 SetContext（32/64 位各前 2000 条）
# 在 WSL 跑: bash /mnt/d/ccz_winlator_patch/unpack_tools/build_ntdll_setprobe.sh
set -e
WINE=/root/wine0905
TOOLS=/mnt/d/ccz_winlator_patch/unpack_tools
OUT=/mnt/d/ccz_winlator_patch/build
V=$WINE/dlls/ntdll/unix/virtual.c
S=$WINE/dlls/ntdll/unix/signal_x86_64.c
BK=/root/ntdll_setprobe_backup_20261108

mkdir -p "$BK"
cp "$V" "$BK/virtual.c"
cp "$S" "$BK/signal_x86_64.c"
restore_src() { cp "$BK/virtual.c" "$V"; cp "$BK/signal_x86_64.c" "$S"; touch "$V" "$S"; }
trap restore_src EXIT

echo "=== 1/3 应用 fix110c + SETPROBE ==="
python3 "$TOOLS/patch_fix110c.py"
python3 - <<'PY'
p = '/root/wine0905/dlls/ntdll/unix/signal_x86_64.c'
s = open(p, encoding='utf-8', errors='replace').read()

# 32 位 Set：在 WCTX write 之后追加文件日志（只 DEBUG 位）
old32 = """            write( 2, wb, wn );
        }
    }


    /* debug registers require a server call */"""
new32 = """            write( 2, wb, wn );
            { static int sn32; if ((context->ContextFlags & CONTEXT_I386_DEBUG_REGISTERS) && sn32++ < 2000) {
              FILE *fp = fopen("/home/xuser/setprobe.log", "a");
              if (fp) { fprintf(fp, "SET32 flags=%lx dr0=%lx dr1=%lx dr7=%lx\\n",
                (unsigned long)context->ContextFlags, (unsigned long)context->Dr0,
                (unsigned long)context->Dr1, (unsigned long)context->Dr7); fclose(fp); } } }
        }
    }


    /* debug registers require a server call */"""
assert s.count(old32) == 1, '32 anchor=%d' % s.count(old32)
s = s.replace(old32, new32, 1)

# 64 位 Set：入口处（只 DEBUG 位）
old64 = """    NTSTATUS ret = STATUS_SUCCESS;
    DWORD flags = context->ContextFlags & ~CONTEXT_AMD64;"""
new64 = """    NTSTATUS ret = STATUS_SUCCESS;
    DWORD flags = context->ContextFlags & ~CONTEXT_AMD64;
    { static int sn64; if ((context->ContextFlags & CONTEXT_DEBUG_REGISTERS) && sn64++ < 2000) {
      FILE *fp = fopen("/home/xuser/setprobe.log", "a");
      if (fp) { fprintf(fp, "SET64 flags=%lx dr0=%lx dr1=%lx dr7=%lx\\n",
        (unsigned long)context->ContextFlags, (unsigned long)context->Dr0,
        (unsigned long)context->Dr1, (unsigned long)context->Dr7); fclose(fp); } } }"""
assert s.count(old64) == 1, '64 anchor=%d' % s.count(old64)
s = s.replace(old64, new64, 1)

open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('SETPROBE inserted')
PY
touch "$V" "$S"

echo "=== 2/3 编译 ntdll.so ==="
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_setprobe.log 2>&1 || { tail -40 /root/make_setprobe.log; exit 1; }
tail -3 /root/make_setprobe.log
M=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  SETPROBE md5=$M"
cp "$WINE/dlls/ntdll/ntdll.so" "$OUT/ntdll_setprobe.so"

echo "=== 3/3 复原源码并重编 ==="
restore_src
make -C "$WINE" -j8 dlls/ntdll/ntdll.so > /root/make_setprobe_restore.log 2>&1 || { tail -30 /root/make_setprobe_restore.log; exit 1; }
R=$(md5sum "$WINE/dlls/ntdll/ntdll.so" | cut -d' ' -f1)
echo "  restored md5=$R"
trap - EXIT
echo "PASS: build/ntdll_setprobe.so 已产出，源码复原"
