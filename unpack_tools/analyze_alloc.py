#!/usr/bin/env python3
"""分析 hang_raw.log: 失败的分配 + 最后事件."""
import sys
data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\hang_raw.log", "rb").read().decode("utf-8", "replace")
lines = data.splitlines()
out = []
fails = [(i, ln) for i, ln in enumerate(lines) if "st=-" in ln]
out.append("failed allocs: %d" % len(fails))
for i, ln in fails[:20]:
    out.append("@%d  %s" % (i, ln[:170]))
probes = [(i, ln) for i, ln in enumerate(lines) if "ALLOCPROBE" in ln]
out.append("total probes: %d" % len(probes))
out.append("--- last 8 printable lines ---")
for ln in lines[-8:]:
    pr = sum(1 for c in ln if c.isprintable())
    if len(ln) == 0 or pr / max(1, len(ln)) > 0.8:
        out.append(ln[:170])
open(r"C:\Users\MECHREV\AppData\Local\Temp\zcode\alloc_fails.txt", "w", encoding="utf-8").write("\n".join(out))
print("written", len(out), "lines")
