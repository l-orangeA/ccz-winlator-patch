#!/usr/bin/env python3
"""R-E minimal: fix CONTEXT.R13 before NtContinue.

Source evidence:
  wine1010/dlls/ntdll/tests/wow64.c:3113
      ok( context == teb64->TlsSlots[WOW64_TLS_CPURESERVED] + 4, ... )
  wow64cpu/cpu.c syscall_32to64_return reads the I386_CONTEXT via %r13
  (and cpu->Flags via -4(%r13)), so %r13 MUST be cpu_area + 4.

Observed in dispatch_exception CONTEXT dumps: r13 = 0x10020fd24 (a stack
address), r12 = 0x7ffc0000 (TEB-ish) -- i.e. R13 is NOT the cpu context.
Restoring that CONTEXT with NtContinue makes the resume asm read garbage,
which explains why every v3-v8 / R-B variant ended at 0x3f0004b3xx.

This patch ONLY sets context->R13 to cpu_area+4 in the v5 swallow block,
plus a read-only ERR. wow64cpu is rolled back to v8 (single variable).
"""
import sys

EXC = "/root/wine1010/dlls/ntdll/exception.c"
s = open(EXC, encoding="utf-8", errors="replace").read()

old = """#ifdef __i386__
        context->Eip += 1;
#endif
        NtContinue( context, FALSE );
    }
"""
if s.count(old) != 1:
    sys.exit("FAIL: v5 swallow block count=%d" % s.count(old))

new = """#ifdef __i386__
        context->Eip += 1;
#endif
#ifndef __i386__
        {
            void *cpu_area = NtCurrentTeb()->TlsSlots[WOW64_TLS_CPURESERVED];
            if (cpu_area)
            {
                ERR( "PROBE-RE cpu_area=%p old_r13=%lx new_r13=%lx r14=%lx\\n",
                     cpu_area, (ULONG_PTR)context->R13,
                     (ULONG_PTR)cpu_area + 4, (ULONG_PTR)context->R14 );
                context->R13 = (ULONG64)(ULONG_PTR)cpu_area + 4;
            }
            else ERR( "PROBE-RE cpu_area is NULL\\n" );
        }
#endif
        NtContinue( context, FALSE );
    }
"""
s = s.replace(old, new, 1)
open(EXC, "w", encoding="utf-8", newline="\n").write(s)
print("OK: R-E R13 fix applied")
