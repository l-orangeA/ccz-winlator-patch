#!/usr/bin/env python3
"""death_b64log.log: 代码页区域(0x400000-0x600000)的 mprotect 跟踪."""
data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\death_b64log.log", "rb").read().decode("utf-8", "replace")
lines = data.splitlines()
out = []
for i, ln in enumerate(lines):
    if "mprotect(0x4" in ln or "mprotect(0x5" in ln:
        out.append("@%d  %s" % (i, ln[:140]))
print("code-page mprotect lines: %d" % len(out))
for ln in out[:50]:
    print("  " + ln)
# 统计 prot 值分布
import collections
pc = collections.Counter()
for ln in out:
    if "0x" in ln:
        parts = ln.split(", ")
        if len(parts) >= 3:
            pc[parts[2].rstrip(")")] += 1
print("prot distribution:", dict(pc))
