#!/usr/bin/env python3
"""Only allow DynaCall for SEGV whose fault address is in the stack range
(0x30000000-0x40000000), so a fault inside malloc never enters the JIT."""
p = "/tmp/b64prof-build/src/libtools/signals.c"
s = open(p).read()
old = "b64p_allow_dynarec = 1;"
new = ("b64p_allow_dynarec = (sig == SIGTRAP) ||\n"
       "        (sig == SIGSEGV && info && (uintptr_t)info->si_addr >= 0x30000000UL\n"
       "                        && (uintptr_t)info->si_addr <  0x40000000UL);")
if old not in s:
    raise SystemExit("pattern missing")
s = s.replace(old, new, 1)
open(p, "w").write(s)
print("patched: stack-range filter")
