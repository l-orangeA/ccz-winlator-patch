#!/bin/bash
# skip-int3：32 位 dispatch_exception 链尾对未处理断点特例（EIP+1 继续，绕开 NtRaiseException trampoline 崩溃）
set -e
WINE=/root/wine1010
OUT=/mnt/d/ccz_winlator_patch/build
E=/root/wine1010/dlls/ntdll/exception.c
BK=/root/exception.c.bak_skipint3

cp "$E" "$BK"
restore_src() { cp "$BK" "$E"; }
trap restore_src EXIT

echo "=== 1/3 应用 skip-int3 补丁 ==="
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
print('skip-int3 applied')
PY

echo "=== 2/3 编译 ntdll（两 arch PE + unix 侧不动） ==="
make -C "$WINE" -j8 dlls/ntdll >/root/make_skipint3.log 2>&1 || { tail -40 /root/make_skipint3.log; exit 1; }
grep -E 'i386-windows/ntdll.dll|x86_64-windows/ntdll.dll|ntdll.so' /root/make_skipint3.log | tail -5
ls -la "$WINE/dlls/ntdll/i386-windows/ntdll.dll" "$WINE/dlls/ntdll/x86_64-windows/ntdll.dll" 2>/dev/null

echo "=== 3/3 收集产物 ==="
mkdir -p "$OUT/skipint3"
cp "$WINE/dlls/ntdll/i386-windows/ntdll.dll" "$OUT/skipint3/ntdll_i386_windows.dll" 2>/dev/null || echo "i386 dll 产物路径待确认"
cp "$WINE/dlls/ntdll/x86_64-windows/ntdll.dll" "$OUT/skipint3/ntdll_x64_windows.dll" 2>/dev/null || echo "x64 dll 产物路径待确认"
md5sum "$OUT/skipint3/"* 2>/dev/null
trap - EXIT
echo "PASS"
