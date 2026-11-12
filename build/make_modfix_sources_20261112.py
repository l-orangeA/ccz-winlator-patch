#!/usr/bin/env python3
"""Clean production version: fix110b + gate the leftover /proc/self/maps debug
scan behind WINE_HWBP_MODLOG. No timing instrumentation, no other change.

This is the deliverable. The measured 474 us -> 4.4 us came from removing that
scan; timers themselves added overhead, so this build should be equal or faster.
"""
import os
import shutil

SRC = "src/fix110b"
DST = "src/fix110b_modfix"
os.makedirs(DST, exist_ok=True)
for f in ("signal_x86_64.c", "virtual.c", "thread.c"):
    shutil.copy2(os.path.join(SRC, f), os.path.join(DST, f))

p = os.path.join(DST, "signal_x86_64.c")
s = open(p, encoding="utf-8").read()

old = "    if ((ULONG_PTR)RIP_sig(ucontext) >= 0x10000000)\n    {   /* locate the module of a host-side fault */"
if old not in s:
    raise SystemExit("debug block not found")

new = ("    {   /* PERF FIX: this /proc/self/maps scan used to run on EVERY\n"
       "     * fault because the shell's RIP is 0x31f014 > 0x10000000, costing\n"
       "     * ~474 us per fault (open + read up to 256 KB + sscanf per line).\n"
       "     * Now debug-only. */\n"
       "        static int modlog = -1;\n"
       "        if (modlog < 0) modlog = getenv( \"WINE_HWBP_MODLOG\" ) != NULL;\n"
       "        if (!modlog) goto skip_modlog;\n"
       "    }\n"
       "    if ((ULONG_PTR)RIP_sig(ucontext) >= 0x10000000)\n"
       "    {   /* locate the module of a host-side fault */")
s = s.replace(old, new, 1)

old2 = ("            }\n"
        "        }\n"
        "    }\n"
        "    if (hwbp_verbose()) {")
if old2 not in s:
    raise SystemExit("block end not found")
new2 = ("            }\n"
        "        }\n"
        "    }\n"
        "skip_modlog:\n"
        "    if (hwbp_verbose()) {")
s = s.replace(old2, new2, 1)

open(p, "w", encoding="utf-8", newline="\n").write(s)
print("DONE: %s (maps scan gated, no timers)" % DST)
