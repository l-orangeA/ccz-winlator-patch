#!/usr/bin/env python3
"""在 0.4.4 死亡日志里追踪 Dr 值的变化（从 WCTX 和 HWBP_DBG 行）."""
import re

data = open(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\death044.log", "rb").read().decode("utf-8", "replace")
lines = data.splitlines()
out = []
last_dr1 = None
for i, ln in enumerate(lines):
    # WCTX 和 HWBP_DBG 都记录 dr 值
    m = re.search(r"dr1=([0-9a-f]+)", ln)
    if m:
        dr1 = m.group(1)
        if dr1 != last_dr1:
            out.append("@%d  dr1 change: %s -> %s | %s" % (i, last_dr1 or "(none)", dr1, ln[:100]))
            last_dr1 = dr1
    # SEGV fault 的地址
    m2 = re.search(r"SEGV_ALL.*addr=([0-9a-f]+).*err=(\d+)", ln)
    if m2:
        out.append("@%d  FAULT addr=%s err=%s" % (i, m2.group(1), m2.group(2)))
    # hit 计数
    m3 = re.search(r"hwbp_cnt_hit", ln)
    if m3:
        out.append("@%d  HIT: %s" % (i, ln[:100]))
open(r"C:\Users\MECHREV\AppData\Local\Temp\zcode\dr_track.txt", "w", encoding="utf-8").write("\n".join(out[:80]))
print("total lines: %d, tracked: %d" % (len(lines), len(out[:80])))
