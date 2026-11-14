#!/usr/bin/env python3
"""Gate the leftover /proc/self/maps debug scan behind WINE_HWBP_MODLOG.
That block ran on EVERY page fault (shell RIP 0x31f014 > 0x10000000) and cost
~474 us: open + read up to 256 KB + sscanf per line."""
p = "src/fix110b_hprof/signal_x86_64.c"
s = open(p, encoding="utf-8").read()

# 1) cached flag right after the entry timestamp
a = "    double hprof_t0 = hwbp_now_ns();"
if a not in s:
    raise SystemExit("entry marker missing")
s = s.replace(a, a + "\n    static int modlog = -1;\n    if (modlog < 0) modlog = getenv( \"WINE_HWBP_MODLOG\" ) != NULL;", 1)

# 2) make the debug block conditional
old = "    if ((ULONG_PTR)RIP_sig(ucontext) >= 0x10000000)\n    {   /* locate the module of a host-side fault */"
if old not in s:
    raise SystemExit("debug block missing")
new = ("    if (modlog && (ULONG_PTR)RIP_sig(ucontext) >= 0x10000000)\n"
       "    {   /* DEBUG-only: /proc/self/maps scan, ~474 us per fault if enabled */")
s = s.replace(old, new, 1)

open(p, "w", encoding="utf-8", newline="\n").write(s)
print("gated: maps scan now needs WINE_HWBP_MODLOG")
