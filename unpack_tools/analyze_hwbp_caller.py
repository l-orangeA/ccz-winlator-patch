#!/usr/bin/env python3
"""流式分析 HWBP_CALLER：定位 d1=ffce115f 来自哪个调用点（Set/Get，32/64 位）。"""
import collections
import re
from pathlib import Path

P = Path(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\hwbp_caller.log")
CALL = re.compile(r"HWBP_CALLER ret=([0-9a-fx]+) d1=([0-9a-f]+) d7=([0-9a-f]+)")
WCTX = re.compile(r"WCTX ctx=\S+ eip=\S+ esp=\S+ ef=\S+ flags=\S+ dr0=\S+ dr1=([0-9a-f]+)")
GCTX = re.compile(r"GCTX_DBG size=\S+ flags=\S+ eip=")

ret_cnt = collections.Counter()
d1_cnt = collections.Counter()
ffce = []
recent = collections.deque(maxlen=3)
total = 0
n = 0

with P.open("rb") as f:
    for raw in f:
        n += 1
        ln = raw.decode("utf-8", "replace").strip()
        if "WCTX" in ln:
            recent.append(("WCTX", ln[:140]))
        elif "GCTX_DBG" in ln:
            recent.append(("GCTX", ln[:140]))
        m = CALL.search(ln)
        if m:
            total += 1
            ret, d1, d7 = m.group(1), m.group(2), m.group(3)
            ret_cnt[ret] += 1
            d1_cnt[d1] += 1
            if d1 == "ffce115f" and len(ffce) < 8:
                ffce.append((n, ret, d7, list(recent)))
            recent.append(("CALL", ln[:140]))

print("总行数", n, " HWBP_CALLER", total)
print("\n=== ret 分布 top10 ===")
for k, v in ret_cnt.most_common(10):
    print("  ret=%-12s %6d" % (k, v))
print("\n=== d1 分布 top10 ===")
for k, v in d1_cnt.most_common(10):
    print("  d1=%-12s %6d" % (k, v))
print("\n=== d1=ffce115f 的调用点 ===")
for n0, ret, d7, ctx in ffce:
    print("  @%d ret=%s d7=%s" % (n0, ret, d7))
    for tag, ln in ctx:
        print("      %-5s %s" % (tag, ln))
