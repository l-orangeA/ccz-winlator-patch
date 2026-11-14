#!/usr/bin/env python3
"""Read-only probe for the FIRST unix-side exception and the NESTED one.

Goal (§11.5 item 2): determine whether the fatal 0x3f0004b307 event is a
SIGSEGV or SIGTRAP, and which exception record leads into virtual_setup_exception.
No behaviour changes: two ERR lines only. stderr is already redirected by the
hwbp5 constructor to $HOME/wine_dbg_<pid>.log.
"""
import sys

S = "/root/wine1010/dlls/ntdll/unix/signal_x86_64.c"
V = "/root/wine1010/dlls/ntdll/unix/virtual.c"

s = open(S, encoding="utf-8", errors="replace").read()
anchor = """static void setup_raise_exception( ucontext_t *sigcontext, EXCEPTION_RECORD *rec, struct xcontext *xcontext )
{
    void *stack_ptr = (void *)(RSP_sig(sigcontext) & ~15);"""
if s.count(anchor) != 1:
    sys.exit("FAIL: setup_raise_exception anchor count=%d" % s.count(anchor))
probe1 = anchor + """
    ERR( "PROBE-FIRST code=%lx flags=%lx addr=%p info0=%p info1=%p rip=%lx trap=%ld rsp=%p\\n",
         (unsigned long)rec->ExceptionCode, (unsigned long)rec->ExceptionFlags,
         rec->ExceptionAddress, (void *)rec->ExceptionInformation[0],
         (void *)rec->ExceptionInformation[1], (unsigned long)xcontext->c.Rip,
         (long)TRAP_sig(sigcontext), (void *)RSP_sig(sigcontext) );"""
s = s.replace(anchor, probe1, 1)
open(S, "w", encoding="utf-8", newline="\n").write(s)

v = open(V, encoding="utf-8", errors="replace").read()
old = '            ERR( "nested exception on signal stack addr %p stack %p\\n", rec->ExceptionAddress, stack );'
if v.count(old) != 1:
    sys.exit("FAIL: nested ERR line count=%d" % v.count(old))
probe2 = old + """
            ERR( "PROBE-NESTED code=%lx flags=%lx addr=%p info0=%p info1=%p\\n",
                 (unsigned long)rec->ExceptionCode, (unsigned long)rec->ExceptionFlags,
                 rec->ExceptionAddress, (void *)rec->ExceptionInformation[0],
                 (void *)rec->ExceptionInformation[1] );"""
v = v.replace(old, probe2, 1)
open(V, "w", encoding="utf-8", newline="\n").write(v)
print("OK: PROBE-FIRST + PROBE-NESTED installed")
