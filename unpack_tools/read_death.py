#!/usr/bin/env python3
"""读 r112d 死亡日志的关键事件."""
import gzip
data = gzip.open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\r112d_death.gz", "rt", encoding="utf-8", errors="replace").read()
lines = data.splitlines()
out = ["total lines: %d" % len(lines)]
for i, ln in enumerate(lines):
    pr = sum(1 for c in ln if c.isprintable())
    if len(ln) > 0 and pr / max(1, len(ln)) < 0.8:
        continue
    for k in ("NTCONT", "FIX110", "SEGV", "MPROT", "Unhandled", "execve", "ALLOCPROBE",
              "create_view", "View: 0x4", "map_image", "DIED", "exit", "Error", "error",
              "hwbp", "HWBP", "Box64", "BOX64", "Fatal", "died"):
        if k in ln:
            out.append("@%d  %s" % (i, ln[:180]))
            break
# 写到设备路径（避免 cmd 路径问题）
with open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\r112d_death_events.txt", "w", encoding="utf-8", errors="replace") as f:
    f.write("\n".join(out))
print("written %d / %d total" % (len(out), len(lines)))
