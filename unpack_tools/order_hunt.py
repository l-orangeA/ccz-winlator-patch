#!/usr/bin/env python3
"""death_b64log.log 按时间顺序: 代码页 mprotect 与 DEP 故障的相对顺序."""
data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\death_b64log.log", "rb").read().decode("utf-8", "replace")
lines = data.splitlines()
events = []
for i, ln in enumerate(lines):
    if "mprotect(0x4" in ln or "mprotect(0x5" in ln:
        events.append((i, "MPROT", ln[:120]))
    if "SEGV_ALL" in ln and ("403a" in ln or "405880" in ln or "405" in ln):
        events.append((i, "DEP?", ln[:120]))
    if "Unhandled" in ln:
        events.append((i, "UNHANDLED", ln[:120]))
    if "Fast _exit" in ln:
        events.append((i, "EXIT", ln[:80]))
print("total events:", len(events))
for i, kind, ln in events:
    print("@%d %-9s %s" % (i, kind, ln))
