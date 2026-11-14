#!/usr/bin/env python3
"""skip-int3 v3: move the swallow block BEFORE RtlRaiseStatus, and log with ERR.

Rationale (2026-11-09, device log wine_dbg_8765.log / 9171.log):
  handler at 00586F8C returned 0  ->  call_seh_handlers status != STATUS_SUCCESS
  ->  RtlRaiseStatus(status) is reached before the v2 skip block
  ->  re-raise goes through the wow64 NtRaiseException trampoline
  ->  handle_syscall_fault c0000005 -> nested exception on signal stack -> death.

v3 puts the check immediately after call_seh_handlers (before any re-raise),
and replaces TRACE with ERR so it is visible even in -DNDEBUG builds.
"""
import sys

P = "/root/wine1010/dlls/ntdll/exception.c"
s = open(P, encoding="utf-8", errors="replace").read()

marker = "#ifdef __i386__\n    /* SKIP-INT3"
start = s.find(marker)
if start < 0:
    sys.exit("FAIL: v2 skip block not found")
end = s.find("#endif", start)
if end < 0:
    sys.exit("FAIL: #endif not found")
end += len("#endif")

# remove old block (+ the blank line left behind)
s = s[:start] + s[end:]
s = s.replace("\n\n\n    return NtRaiseException", "\n\n    return NtRaiseException")

anchor = "    if (status != STATUS_UNHANDLED_EXCEPTION) RtlRaiseStatus( status );"
if s.count(anchor) != 1:
    sys.exit("FAIL: RtlRaiseStatus anchor count=%d" % s.count(anchor))

block = """#ifdef __i386__
    /* SKIP-INT3 v3: swallow unhandled breakpoint before any re-raise. */
    if (rec->ExceptionCode == EXCEPTION_BREAKPOINT &&
        !(rec->ExceptionFlags & (EXCEPTION_UNWINDING | EXCEPTION_EXIT_UNWIND)) &&
        !(rec->ExceptionFlags & EXCEPTION_NONCONTINUABLE))
    {
        ERR( "SKIP-INT3: swallowing breakpoint at %p status=%lx\\n",
             rec->ExceptionAddress, (unsigned long)status );
        context->Eip += 1;
        NtContinue( context, FALSE );
    }
#endif
"""
s = s.replace(anchor, block + anchor, 1)
open(P, "w", encoding="utf-8", newline="\n").write(s)
print("OK: v3 applied; ERR marker count =", s.count("SKIP-INT3"))
