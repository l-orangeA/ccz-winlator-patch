#!/bin/bash
# skip-int3 v2：修正顺序（补丁→构建→收集→恢复）
set -e
WINE=/root/wine1010
OUT=/mnt/d/ccz_winlator_patch/build
E=/root/wine1010/dlls/ntdll/exception.c
BK=/root/exception.c.bak_skipint3
cp "$E" "$BK"

echo "=== 1/4 打补丁 ==="
python3 - <<'PY'
p = '/root/wine1010/dlls/ntdll/exception.c'
s = open(p, encoding='utf-8', errors='replace').read()
old = '''    if (status != STATUS_UNHANDLED_EXCEPTION) RtlRaiseStatus( status );
    return NtRaiseException( rec, context, FALSE );'''
new = '''    if (status != STATUS_UNHANDLED_EXCEPTION) RtlRaiseStatus( status );
#ifdef __i386__
    /* SKIP-INT3: VProtect-style shells probe the SEH chain with an int3 and
     * expect the system to swallow unhandled breakpoints. On wow64/ARM hosts
     * the NtRaiseException tail goes through the cpu-backend trampoline and
     * faults re-entrantly on the signal stack, killing the process. Skip the
     * int3 (0xCC) and resume instead. */
    if (rec->ExceptionCode == EXCEPTION_BREAKPOINT &&
        !(rec->ExceptionFlags & (EXCEPTION_UNWINDING | EXCEPTION_EXIT_UNWIND)) &&
        !(rec->ExceptionFlags & EXCEPTION_NONCONTINUABLE))
    {
        TRACE( "SKIP-INT3: swallowing unhandled breakpoint at %p\\n", rec->ExceptionAddress );
        context->Eip += 1;
        NtContinue( context, FALSE );
    }
#endif
    return NtRaiseException( rec, context, FALSE );'''
assert s.count(old) == 1, 'anchor=%d' % s.count(old)
s = s.replace(old, new, 1)
open(p, 'w', encoding='utf-8', newline='\n').write(s)
print('applied')
PY
N=$(grep -c 'SKIP-INT3' "$E")
echo "patch marker count: $N (expect 1)"
[ "$N" = "2" ] || { echo PATCH_FAILED; exit 1; }
touch "$E"

echo "=== 2/4 构建 ==="
make -C "$WINE" -j8 dlls/ntdll/i386-windows/ntdll.dll dlls/ntdll/x86_64-windows/ntdll.dll > /root/make_skipint3c.log 2>&1 || { tail -30 /root/make_skipint3c.log; exit 1; }
ls -la "$WINE/dlls/ntdll/i386-windows/ntdll.dll" "$WINE/dlls/ntdll/x86_64-windows/ntdll.dll"

echo "=== 3/4 收集产物 ==="
mkdir -p "$OUT/skipint3"
cp "$WINE/dlls/ntdll/i386-windows/ntdll.dll" "$OUT/skipint3/ntdll_i386_windows.dll"
cp "$WINE/dlls/ntdll/x86_64-windows/ntdll.dll" "$OUT/skipint3/ntdll_x64_windows.dll"
md5sum "$OUT/skipint3/ntdll_i386_windows.dll" "$OUT/skipint3/ntdll_x64_windows.dll"
strings "$OUT/skipint3/ntdll_i386_windows.dll" | grep -c 'SKIP-INT3' && echo "marker in binary OK"

echo "=== 4/4 恢复源码 ==="
cp "$BK" "$E"; touch "$E"
echo "PASS"
