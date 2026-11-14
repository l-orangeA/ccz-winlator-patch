#!/usr/bin/env python3
"""在 death_b64log.log 里找 box64 崩溃转储与信号相关行."""
data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\death_b64log.log", "rb").read().decode("utf-8", "replace")
lines = data.splitlines()
pats = ["Block is:", "Segmentation", "SIGSEGV", "SIGILL", "SIGABRT", "double fault", "Double SIGSEGV",
        "RIP=", "opcode", "Signal", "Exited", "exit", "x64emu", "native call", "Custom"]
hits = [(i, ln) for i, ln in enumerate(lines) if any(p in ln for p in pats)]
print("total lines: %d, signal/crash hits: %d" % (len(lines), len(hits)))
for i, ln in hits[-25:]:
    print("@%d  %s" % (i, ln[:200]))
# 最后 200 行里的非 mprotect 行
print("=== last 200 lines, non-mprotect-set lines ===")
shown = 0
for ln in lines[-200:]:
    if "set memprot" in ln or "set mapallmem" in ln or "mprotect(0x" in ln:
        continue
    if shown < 30:
        print("  " + ln[:200])
        shown += 1
