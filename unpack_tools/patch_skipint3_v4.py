#!/usr/bin/env python3
"""skip-int3 v4: patch the modules that ACTUALLY run in Wine 10 new wow64.

Evidence (2026-11-09):
  - i386-windows/ntdll.dll is NOT on the 32-bit SEH dispatch path (v2/v3 ERR never fired).
  - 32-bit context is owned by wow64cpu.dll: BTCpuResetToConsistentState() copies the
    64-bit CONTEXT via copy_context_64to32(), where Eip = Rip (low 32 bits),
    then BTCpuSetContext() stores it before fixing up Rip to syscall_32to64.
  - The fatal path is the 64-bit PE ntdll dispatch_exception() tail:
    handler returned 0 -> RtlRaiseStatus / NtRaiseException -> wow64 trampoline
    -> handle_syscall_fault c0000005 -> nested exception -> death.

v4 = two coordinated changes:
  A. ntdll/exception.c (x86_64 PE): swallow unhandled EXCEPTION_BREAKPOINT from
     32-bit guest code (<0x10000000) by NtContinue() BEFORE any re-raise.
  B. wow64cpu/cpu.c: advance wow_context.Eip by 1 for the same breakpoint, so
     syscall_32to64_return resumes AFTER the int3 instead of looping.
"""
import sys

EXC = "/root/wine1010/dlls/ntdll/exception.c"
CPU = "/root/wine1010/dlls/wow64cpu/cpu.c"

# ---------- A. exception.c ----------
s = open(EXC, encoding="utf-8", errors="replace").read()
start = s.find("#ifdef __i386__\n    /* SKIP-INT3")
if start < 0:
    sys.exit("FAIL A: v3 block not found")
end = s.find("#endif", start) + len("#endif")
s = s[:start] + s[end:]
s = s.replace("\n\n    if (status != STATUS_UNHANDLED_EXCEPTION)",
              "\n    if (status != STATUS_UNHANDLED_EXCEPTION)")

anchor = "    if (status != STATUS_UNHANDLED_EXCEPTION) RtlRaiseStatus( status );"
if s.count(anchor) != 1:
    sys.exit("FAIL A: anchor count=%d" % s.count(anchor))

block = """    /* SKIP-INT3 v4: VProtect anti-debug int3 from 32-bit guest code.
     * wow64cpu already advanced the 32-bit Eip; just resume instead of
     * re-raising through the fatal NtRaiseException trampoline. */
    if (rec->ExceptionCode == EXCEPTION_BREAKPOINT &&
        (ULONG_PTR)rec->ExceptionAddress < 0x10000000 &&
        !(rec->ExceptionFlags & (EXCEPTION_UNWINDING | EXCEPTION_EXIT_UNWIND)) &&
        !(rec->ExceptionFlags & EXCEPTION_NONCONTINUABLE))
    {
        ERR( "SKIP-INT3 v4: resuming after breakpoint at %p status=%lx\\n",
             rec->ExceptionAddress, (unsigned long)status );
#ifdef __i386__
        context->Eip += 1;
#endif
        NtContinue( context, FALSE );
    }
"""
s = s.replace(anchor, block + anchor, 1)
open(EXC, "w", encoding="utf-8", newline="\n").write(s)

# ---------- B. wow64cpu/cpu.c ----------
c = open(CPU, encoding="utf-8", errors="replace").read()
anchor2 = "    wow_context.EFlags &= ~(0x100|0x40000);"
if c.count(anchor2) != 1:
    sys.exit("FAIL B: EFlags anchor count=%d" % c.count(anchor2))
block2 = """    wow_context.EFlags &= ~(0x100|0x40000);
    /* SKIP-INT3 v4: step over the one-byte int3 probe in 32-bit guest code. */
    if (ptrs->ExceptionRecord->ExceptionCode == EXCEPTION_BREAKPOINT &&
        (ULONG_PTR)ptrs->ExceptionRecord->ExceptionAddress < 0x10000000)
    {
        ERR( "SKIP-INT3 cpu: Eip %lx -> %lx\\n", wow_context.Eip, wow_context.Eip + 1 );
        wow_context.Eip += 1;
    }
"""
c = c.replace(anchor2, block2, 1)
open(CPU, "w", encoding="utf-8", newline="\n").write(c)
print("OK v4 applied: exception.c + wow64cpu/cpu.c")
