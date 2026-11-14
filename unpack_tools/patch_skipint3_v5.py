#!/usr/bin/env python3
"""skip-int3 v5: move the 64-bit PE ntdll swallow block BEFORE call_seh_handlers.

v4 evidence (wine_dbg_11197.log):
  - wow64cpu BTCpuResetToConsistentState fired: Eip 587033 -> 587034
  - 2nd dispatch shows eip=00587034  (Eip+1 really landed)
  - but `handler at 00586F8C returned 0` -> handle_syscall_fault c0000005
    happens INSIDE call_seh_handlers / its 32-bit unwind path, so the old
    block placed after call_seh_handlers was never reached (no v4 ERR line).
v5 intercepts the breakpoint right after vectored handlers, before SEH.
"""
import sys

EXC = "/root/wine1010/dlls/ntdll/exception.c"
s = open(EXC, encoding="utf-8", errors="replace").read()

start = s.find("    /* SKIP-INT3 v4:")
if start < 0:
    sys.exit("FAIL: v4 block not found")
end = s.find("    if (status != STATUS_UNHANDLED_EXCEPTION)", start)
if end < 0:
    sys.exit("FAIL: end anchor not found")
block = s[start:end].replace("SKIP-INT3 v4", "SKIP-INT3 v5")
s = s[:start] + s[end:]

anchor = "    if (call_vectored_handlers( rec, context ) == EXCEPTION_CONTINUE_EXECUTION)"
if s.count(anchor) != 1:
    sys.exit("FAIL: vectored anchor count=%d" % s.count(anchor))
s = s.replace(anchor, block + anchor, 1)
open(EXC, "w", encoding="utf-8", newline="\n").write(s)
print("OK v5: swallow block moved before call_seh_handlers")
