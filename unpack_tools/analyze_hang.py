#!/usr/bin/env python3
"""分析 r113 挂死日志: 事件类型统计 + 最后 60 行."""
import gzip, collections, sys

path = r"D:\ccz_winlator_patch\unpack_tools\a0_trace\hang_tail2.log.gz"
lines = gzip.open(path, "rt", encoding="utf-8", errors="replace").read().splitlines()
print("total lines:", len(lines))
# 事件前缀统计
pref = collections.Counter()
for ln in lines:
    key = ln.split(" ")[0][:24] if ln else "?"
    pref[key] += 1
for k, c in pref.most_common(15):
    print("%7d  %s" % (c, k))
print("=== last 50 lines ===")
for ln in lines[-50:]:
    print(ln[:200])
