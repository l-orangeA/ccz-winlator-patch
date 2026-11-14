#!/usr/bin/env python3
"""skip-int3 v6: keep the 32-bit context for NtContinue (skip the 64-bit fixup).

v5 evidence (wine_dbg_11659.log):
  SKIP-INT3 cpu Eip 587033 -> 587034
  SKIP-INT3 v5 resuming after breakpoint
  Wow64SystemServiceEx unsupported syscall 586f8c
  -> c0000005 at C000001C
Reason: BTCpuResetToConsistentState() fakes a 64-bit syscall entry
(context->Rip = syscall_32to64, SegCs = cs64_sel). NtContinue() to that
context re-enters the syscall_32to64 PROLOG, which interprets %eax as a
syscall number. For a breakpoint there is no syscall to resume.

v6: for guest int3, advance Eip in BOTH the 32-bit and 64-bit views and
return WITHOUT the 64-bit fixup, so NtContinue() restores the original
32-bit CS/Eip through Wine's normal wow64 restore path.
"""
import sys

CPU = "/root/wine1010/dlls/wow64cpu/cpu.c"
c = open(CPU, encoding="utf-8", errors="replace").read()

old = """    /* SKIP-INT3 v4: step over the one-byte int3 probe in 32-bit guest code. */
    if (ptrs->ExceptionRecord->ExceptionCode == EXCEPTION_BREAKPOINT &&
        (ULONG_PTR)ptrs->ExceptionRecord->ExceptionAddress < 0x10000000)
    {
        ERR( "SKIP-INT3 cpu: Eip %lx -> %lx\\n", wow_context.Eip, wow_context.Eip + 1 );
        wow_context.Eip += 1;
    }
"""
if c.count(old) != 1:
    sys.exit("FAIL: v4 cpu block count=%d" % c.count(old))

new = """    /* SKIP-INT3 v6: step over the one-byte int3 probe in 32-bit guest code,
     * and keep the 32-bit context so NtContinue() can restore it directly. */
    if (ptrs->ExceptionRecord->ExceptionCode == EXCEPTION_BREAKPOINT &&
        (ULONG_PTR)ptrs->ExceptionRecord->ExceptionAddress < 0x10000000)
    {
        ERR( "SKIP-INT3 cpu v6: Eip %lx -> %lx (no fixup)\\n",
             wow_context.Eip, wow_context.Eip + 1 );
        wow_context.Eip += 1;
        context->Rip += 1;
        BTCpuSetContext( GetCurrentThread(), GetCurrentProcess(), NULL, &wow_context );
        return STATUS_SUCCESS;
    }
"""
c = c.replace(old, new, 1)
open(CPU, "w", encoding="utf-8", newline="\n").write(c)
print("OK v6 applied to wow64cpu/cpu.c")
