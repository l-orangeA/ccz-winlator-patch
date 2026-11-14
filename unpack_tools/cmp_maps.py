#!/usr/bin/env python3
"""对比两版 maps: 0x70a000-0x30110000 区间的映射."""
m30 = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\maps_030.txt", encoding="utf-8", errors="replace").read().splitlines()
# 0.4.4 的 maps 只在日志里(前 40 行),重新格式化 0.3.0 的区间视图
lo, hi = 0x70a000, 0x30110000
print("=== 0.3.0: mappings overlapping 0x70a000-0x30110000 ===")
cnt = 0
for ln in m30:
    try:
        a, b = ln.split()[0].split("-")
        a, b = int(a, 16), int(b, 16)
    except Exception:
        continue
    if a < hi and b > lo:
        print("  " + ln[:150])
        cnt += 1
print("  (total %d)" % cnt)
# 统计 0.3.0 各区总量
tot_rwx = sum(1 for ln in m30 if "rwxp" in ln)
tot_none = sum(1 for ln in m30 if "---p" in ln)
print("0.3.0 total rwxp=%d ---p=%d lines=%d" % (tot_rwx, tot_none, len(m30)))
