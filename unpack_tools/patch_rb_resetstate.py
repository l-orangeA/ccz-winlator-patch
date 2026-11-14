#!/usr/bin/env python3
"""R-B single-variable: set WOW64_CPURESERVED_FLAG_RESET_STATE before NtContinue.

Baseline = v8 (wow64cpu md5 ce69f435...). v8 already:
  - advances wow_context.Eip by 1
  - keeps the 64-bit fixup (Rip = syscall_32to64 prolog)
  - writes Eip+1 into the 32-bit stack top read by that prolog

Missing piece: syscall_32to64_return does `btrl $0,-4(%r13)` on
cpu->Flags. If RESET_STATE is set, it takes `.Lsyscall_32to64_return`
which rebuilds the 32-bit frame with iretq from wow_frame; otherwise it
falls to `ljmp *(%r14)` (the path that crashes).

This patch ONLY adds the flag assignment plus two read-only ERR lines.
"""
import sys

CPU = "/root/wine1010/dlls/wow64cpu/cpu.c"
c = open(CPU, encoding="utf-8", errors="replace").read()

old = """        *(DWORD *)(ULONG_PTR)context->R14 = (DWORD)wow_context.Eip;
        ERR( "SKIP-INT3 v8: stack top set to %lx\\n", (ULONG_PTR)wow_context.Eip );
"""
if c.count(old) != 1:
    sys.exit("FAIL: v8 block count=%d" % c.count(old))

new = old + """        {
            WOW64_CPURESERVED *cpu = (WOW64_CPURESERVED *)NtCurrentTeb()->TlsSlots[WOW64_TLS_CPURESERVED];
            ERR( "PROBE-RB flags_before=%lx eip=%lx\\n",
                 cpu ? (ULONG_PTR)cpu->Flags : 0, (ULONG_PTR)wow_context.Eip );
            if (cpu) cpu->Flags |= WOW64_CPURESERVED_FLAG_RESET_STATE;
            ERR( "PROBE-RB flags_after=%lx\\n", cpu ? (ULONG_PTR)cpu->Flags : 0 );
        }
"""
c = c.replace(old, new, 1)
open(CPU, "w", encoding="utf-8", newline="\n").write(c)
print("OK: R-B RESET_STATE single-variable patch applied")
