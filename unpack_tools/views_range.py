#!/usr/bin/env python3
"""检查 fix110b 注册的视图是否有覆盖 0x400000-0x425000（镜像区）的."""
import re

data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\hang_raw.log", "rb").read().decode("utf-8", "replace")
views = []
for ln in data.splitlines():
    m = re.match(r"FIX110 view ([0-9a-f]+)-([0-9a-f]+) (\S+)", ln)
    if m:
        a, b = int(m.group(1), 16), int(m.group(2), 16)
        views.append((a, b, m.group(3)))
print("total fix110b views:", len(views))
conflict = [(a, b, p) for a, b, p in views if a < 0x425000 and b > 0x400000]
print("views overlapping image region (0x400000-0x425000):", len(conflict))
for a, b, p in conflict:
    print("  CONFLICT: %x-%x %s" % (a, b, p))
# 排序显示所有视图（低区）
print("\n=== all views sorted by addr (first 30) ===")
for a, b, p in sorted(views)[:30]:
    print("  %06x-%06x %s" % (a, b, p))
