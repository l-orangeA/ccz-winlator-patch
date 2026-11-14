#!/usr/bin/env python3
"""box64 0.4.4 补丁 r112d: SIGTRAP 不可延迟。
在 /root/box64-0.4.4ntf 树上运行（已有 r113 glitch 修复 + fix110c）。
在 WSL 跑: python3 patch_r112d.py
"""
import sys, os

ROOT = "/root/box64-0.4.4ntf"
P = ROOT + "/src/libtools/signals.c"

OLD = '''static int is_signal_deferrable(int sig)
{
    switch (sig) {
        case X64_SIGSEGV:
        case X64_SIGBUS:
        case X64_SIGILL:
        case X64_SIGABRT:
            return 0;
    }
    return 1;
}'''

NEW = '''static int is_signal_deferrable(int sig)
{
    switch (sig) {
        case X64_SIGSEGV:
        case X64_SIGBUS:
        case X64_SIGILL:
        case X64_SIGABRT:
        case X64_SIGTRAP:  /* r112d: hwbp sim needs immediate forwarding with exact RIP */
            return 0;
    }
    return 1;
}'''

src = open(P, encoding="utf-8", errors="replace").read()
if "r112d" in src:
    print("already patched")
elif src.count(OLD) == 1:
    open(P, "w", encoding="utf-8", newline="\n").write(src.replace(OLD, NEW))
    print("patched")
else:
    raise SystemExit("anchor count=%d" % src.count(OLD))
