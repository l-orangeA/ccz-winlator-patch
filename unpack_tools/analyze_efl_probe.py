#!/usr/bin/env python3
"""分析 EFL_PROBE 日志：Wine 清掉的 TF 是否被 old_eflags 覆盖。"""
import collections
import re
from pathlib import Path

P = Path(r"D:\ccz_winlator_patch\unpack_tools\a0_trace\efl_probe.log")
RE = re.compile(r"n=(\d+) sig=(\d+) old=([0-9a-f]+)\(tf=(\d)\) ctx=([0-9a-f]+)\(tf=(\d)\) "
                r"copy=([0-9a-f]+)\(tf=(\d)\) curdb=(\d) changed=(\d)")

rows = []
for ln in P.read_text(encoding="utf-8", errors="replace").splitlines():
    m = RE.search(ln)
    if m:
        rows.append(dict(n=int(m.group(1)), sig=int(m.group(2)), oldtf=int(m.group(4)),
                         ctxtf=int(m.group(6)), copytf=int(m.group(8)),
                         curdb=int(m.group(9)), changed=int(m.group(10)), raw=ln))

print("总有效行", len(rows))
print("\n=== sig 分布 ===")
for s, c in collections.Counter(r["sig"] for r in rows).most_common():
    print("  sig=%-3d %5d" % (s, c))

print("\n=== (old_tf, ctx_tf, copy_tf, changed, curdb) 组合分布 ===")
key = lambda r: (r["oldtf"], r["ctxtf"], r["copytf"], r["changed"], r["curdb"])
for k, c in collections.Counter(key(r) for r in rows).most_common(15):
    print("  old=%d ctx=%d copy=%d changed=%d curdb=%d  -> %5d" % (k[0], k[1], k[2], k[3], k[4], c))

print("\n=== 关键判据：Wine 清了 TF（ctx=0 / copy=1）但 emu 恢复成 old=1 ===")
bad = [r for r in rows if r["copytf"] == 1 and r["ctxtf"] == 0 and r["oldtf"] == 1]
print("  命中行数:", len(bad))
for r in bad[:12]:
    print("   ", r["raw"])

print("\n=== SIGTRAP(sig=5) 样本：前 20 / 后 20 ===")
traps = [r for r in rows if r["sig"] == 5]
print("  SIGTRAP 行数:", len(traps))
for r in traps[:20]:
    print("   ", r["raw"])
print("  ...")
for r in traps[-20:]:
    print("   ", r["raw"])

print("\n=== TF=1 出现的信号分布 ===")
for s, c in collections.Counter(r["sig"] for r in rows if r["oldtf"] or r["ctxtf"]).most_common():
    print("  sig=%-3d %5d" % (s, c))
